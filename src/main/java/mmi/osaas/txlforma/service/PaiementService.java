package mmi.osaas.txlforma.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.enums.PaiementStatus;
import mmi.osaas.txlforma.enums.PanierStatus;
import mmi.osaas.txlforma.model.Paiement;
import mmi.osaas.txlforma.model.Panier;
import mmi.osaas.txlforma.model.PanierSession;
import mmi.osaas.txlforma.model.Participation;
import mmi.osaas.txlforma.repository.PaiementRepository;
import mmi.osaas.txlforma.repository.PanierRepository;
import mmi.osaas.txlforma.repository.PanierSessionRepository;
import mmi.osaas.txlforma.repository.ParticipationRepository;
import mmi.osaas.txlforma.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaiementService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    private final PaiementRepository paiementRepository;
    private final PanierRepository panierRepository;
    private final PanierSessionRepository panierSessionRepository;
    private final ParticipationRepository participationRepository;
    private final SessionRepository sessionRepository;
    
    private static final Map<String, Object> PAYMENT_LOCKS = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public Map<String, String> createCheckoutSession(Long userId, Long panierId) {
        Panier panier = getOrFindPanier(userId, panierId);
        List<PanierSession> panierSessions = panierSessionRepository.findByPanierId(panier.getId());
        if (panierSessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le panier est vide");
        }

        try {
            List<SessionCreateParams.LineItem> lineItems = panierSessions.stream()
                    .map(panierSession -> {
                        mmi.osaas.txlforma.model.Session session = panierSession.getSession();
                        String sessionName = session.getFormation() != null 
                                ? session.getFormation().getTitle() + " - Session" 
                                : "Session";
                        return SessionCreateParams.LineItem.builder()
                                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("eur")
                                        .setUnitAmount(Math.round(session.getPrice() * 100))
                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(sessionName)
                                                .build())
                                        .build())
                                .setQuantity(1L)
                                .build();
                    })
                    .toList();

            String sessionIds = panierSessions.stream()
                    .map(panierSession -> panierSession.getSession().getId().toString())
                    .collect(Collectors.joining(","));

            Session checkoutSession = Session.create(SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(baseUrl + "/panier?payment_success=true&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(baseUrl + "/panier")
                    .setCustomerEmail(panier.getUser().getEmail())
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .addAllLineItem(lineItems)
                    .putMetadata("panier_id", panier.getId().toString())
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("session_ids", sessionIds)
                    .build());

            Map<String, String> response = new HashMap<>();
            response.put("sessionId", checkoutSession.getId());
            response.put("url", checkoutSession.getUrl());
            return response;
        } catch (StripeException stripeException) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur Stripe");
        }
    }

    private Panier getOrFindPanier(Long userId, Long panierId) {
        if (panierId != null) {
            return panierRepository.findById(panierId)
                    .filter(panier -> panier.getUser().getId().equals(userId) && panier.getStatus() == PanierStatus.EN_COURS)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Panier invalide"));
        }
        return panierRepository.findByUserIdAndStatus(userId, PanierStatus.EN_COURS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Panier introuvable"));
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        try {
            com.stripe.model.Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            
            if (!"checkout.session.completed".equals(event.getType())) {
                return;
            }

            Session checkoutSession = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "CheckoutSession introuvable"));
            
            String panierIdStr = checkoutSession.getMetadata().get("panier_id");
            String paymentIntentId = checkoutSession.getPaymentIntent();
            
            if (panierIdStr == null || paymentIntentId == null) {
                return;
            }
            
            Object paymentLock = PAYMENT_LOCKS.computeIfAbsent(paymentIntentId, k -> new Object());
            synchronized (paymentLock) {
                try {
                    processPayment(checkoutSession, paymentIntentId, panierIdStr);
                } finally {
                    PAYMENT_LOCKS.remove(paymentIntentId);
                }
            }
        } catch (StripeException stripeException) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erreur Stripe");
        }
    }

    @Transactional
    public void syncCheckoutSession(String sessionId, Long userId) {
        try {
            Session checkoutSession = Session.retrieve(sessionId);
            
            if (!"complete".equals(checkoutSession.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La session de paiement n'est pas complète");
            }
            
            String paymentIntentId = checkoutSession.getPaymentIntent();
            if (paymentIntentId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PaymentIntent introuvable");
            }
            
            Object paymentLock = PAYMENT_LOCKS.computeIfAbsent(paymentIntentId, k -> new Object());
            synchronized (paymentLock) {
                try {
                    String panierIdStr = checkoutSession.getMetadata().get("panier_id");
                    if (panierIdStr == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Panier introuvable");
                    }
                    
                    Panier panier = panierRepository.findById(Long.parseLong(panierIdStr))
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Panier introuvable"));
                    
                    if (!panier.getUser().getId().equals(userId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
                    }
                    
                    Optional<Paiement> existingPaiement = paiementRepository.findByPaymentIntentId(paymentIntentId);
                    if (existingPaiement.isPresent()) {
                        Paiement paiement = existingPaiement.get();
                        if (!paiement.getUser().getId().equals(userId)) {
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
                        }
                    }
                    
                    processPayment(checkoutSession, paymentIntentId, panierIdStr);
                } finally {
                    PAYMENT_LOCKS.remove(paymentIntentId);
                }
            }
        } catch (StripeException stripeException) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur Stripe: " + stripeException.getMessage());
        } catch (Exception generalException) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la synchronisation: " + generalException.getMessage());
        }
    }

    @Transactional
    private void processPayment(Session checkoutSession, String paymentIntentId, String panierIdStr) {
        Optional<Paiement> existingPaiementOpt = paiementRepository.findByPaymentIntentId(paymentIntentId);
        Paiement paiement;
        Panier panier;
        
        if (existingPaiementOpt.isPresent()) {
            paiement = existingPaiementOpt.get();
            panier = paiement.getPanier();
        } else {
            panier = panierRepository.findById(Long.parseLong(panierIdStr))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Panier introuvable"));
            
            String paymentMethod = getPaymentMethod(paymentIntentId);
            
            try {
                paiement = Paiement.builder()
                        .user(panier.getUser())
                        .panier(panier)
                        .amount(Double.parseDouble(checkoutSession.getAmountTotal().toString()) / 100.0)
                        .currency("eur")
                        .paymentIntentId(paymentIntentId)
                        .paymentMethod(paymentMethod)
                        .status(PaiementStatus.SUCCEEDED)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                paiement = paiementRepository.save(paiement);
            } catch (DataIntegrityViolationException integrityException) {
                paiement = paiementRepository.findByPaymentIntentId(paymentIntentId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
            }
        }
        
        List<Participation> existingParticipations = participationRepository.findByPaiementId(paiement.getId());
        if (!existingParticipations.isEmpty()) {
            updatePanierStatus(panier.getId());
            return;
        }
        
        String sessionIdsStr = checkoutSession.getMetadata().get("session_ids");
        if (sessionIdsStr != null && !sessionIdsStr.isEmpty()) {
            createParticipations(paiement, sessionIdsStr);
        }
        
        updatePanierStatus(panier.getId());
    }

    @Transactional
    private void createParticipations(Paiement paiement, String sessionIdsStr) {
        if (sessionIdsStr == null || sessionIdsStr.isEmpty()) {
            return;
        }
        
        String[] sessionIds = sessionIdsStr.split(",");
        
        for (String sessionIdStr : sessionIds) {
            try {
                Long sessionId = Long.parseLong(sessionIdStr.trim());
                mmi.osaas.txlforma.model.Session session = sessionRepository.findById(sessionId).orElse(null);
                if (session == null) {
                    continue;
                }
                
                participationRepository.save(Participation.builder()
                        .user(paiement.getUser())
                        .session(session)
                        .paiement(paiement)
                        .status(mmi.osaas.txlforma.enums.ParticipationStatus.INSCRIT)
                        .createdAt(LocalDateTime.now())
                        .build());
            } catch (DataIntegrityViolationException integrityException) {
            } catch (NumberFormatException numberFormatException) {
            }
        }
    }

    private String getPaymentMethod(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            String paymentMethodId = paymentIntent.getPaymentMethod();
            
            if (paymentMethodId != null) {
                com.stripe.model.PaymentMethod paymentMethod = com.stripe.model.PaymentMethod.retrieve(paymentMethodId);
                String paymentType = paymentMethod.getType();
                
                if ("apple_pay".equals(paymentType)) {
                    return "apple_pay";
                }
                
                com.stripe.model.PaymentMethod.Card card = paymentMethod.getCard();
                if (card != null) {
                    com.stripe.model.PaymentMethod.Card.Wallet wallet = card.getWallet();
                    if (wallet != null && wallet.toString().contains("apple_pay")) {
                        return "apple_pay";
                    }
                }
            }
            
            String latestChargeId = paymentIntent.getLatestCharge();
            if (latestChargeId != null) {
                Charge charge = Charge.retrieve(latestChargeId);
                if (charge.getPaymentMethodDetails() != null) {
                    String chargeType = charge.getPaymentMethodDetails().getType();
                    if ("apple_pay".equals(chargeType)) {
                        return "apple_pay";
                    }
                }
            }
        } catch (StripeException stripeException) {
        }
        return "carte";
    }

    @Transactional
    private void updatePanierStatus(Long panierId) {
        Panier panier = panierRepository.findById(panierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Panier introuvable"));

        List<PanierSession> sessions = panierSessionRepository.findByPanierId(panier.getId());
        if (!sessions.isEmpty()) {
            panierSessionRepository.deleteByPanierId(panier.getId());
        }
        
        if (panier.getStatus() != PanierStatus.EN_COURS) {
            panier.setStatus(PanierStatus.EN_COURS);
        }
        panier.setUpdatedAt(LocalDateTime.now());
        panierRepository.save(panier);
    }
}
