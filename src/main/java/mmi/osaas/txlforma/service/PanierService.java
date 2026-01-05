package mmi.osaas.txlforma.service;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.PanierDTO;
import mmi.osaas.txlforma.dto.PanierSessionDTO;
import mmi.osaas.txlforma.dto.SessionDTO;
import mmi.osaas.txlforma.enums.PanierStatus;
import mmi.osaas.txlforma.model.Panier;
import mmi.osaas.txlforma.model.PanierSession;
import mmi.osaas.txlforma.model.Participation;
import mmi.osaas.txlforma.model.Session;
import mmi.osaas.txlforma.model.User;
import mmi.osaas.txlforma.enums.AttestationType;
import mmi.osaas.txlforma.repository.AttestationRepository;
import mmi.osaas.txlforma.repository.PanierRepository;
import mmi.osaas.txlforma.repository.PanierSessionRepository;
import mmi.osaas.txlforma.repository.ParticipationRepository;
import mmi.osaas.txlforma.repository.SessionRepository;
import mmi.osaas.txlforma.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PanierService {

    private final PanierRepository panierRepository;
    private final PanierSessionRepository panierSessionRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final AttestationRepository attestationRepository;

    public PanierDTO getPanierByUserId(Long userId) {
        Panier panier = panierRepository.findByUserIdAndStatus(userId, PanierStatus.EN_COURS)
                .orElseGet(() -> createPanierForUser(userId));
        
        return convertToDTO(panier);
    }

    @Transactional
    public PanierDTO addSessionToPanier(Long userId, Long sessionId) {
        Panier panier = panierRepository.findByUserIdAndStatus(userId, PanierStatus.EN_COURS)
                .orElseGet(() -> createPanierForUser(userId));

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session introuvable"));

        if (panierSessionRepository.existsByPanierIdAndSessionId(panier.getId(), sessionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette session est déjà dans votre panier");
        }

        if (participationRepository.existsByUserIdAndSessionId(userId, sessionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Vous avez déjà payé cette session. Vous ne pouvez pas l'ajouter à nouveau au panier.");
        }

        List<Participation> validParticipations = participationRepository.findValidByUserIdAndFormationId(
                userId, session.getFormation().getId());
        boolean hasSuccessAttestation = validParticipations.stream()
                .anyMatch(participation -> attestationRepository
                        .findByParticipationIdAndType(participation.getId(), AttestationType.SUCCES)
                        .isPresent());
        if (hasSuccessAttestation) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Vous avez déjà validé cette formation avec succès. Vous ne pouvez pas vous réinscrire.");
        }

        checkSessionAvailability(session);

        List<PanierSession> existingSessions = panierSessionRepository.findByPanierId(panier.getId());
        existingSessions.stream()
                .filter(panierSession -> sessionsOverlap(session, panierSession.getSession()))
                .findFirst()
                .ifPresent(overlappingSession -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Cette session chevauche avec la session " + overlappingSession.getSession().getId() + 
                            " déjà dans votre panier");
                });

        PanierSession panierSession = PanierSession.builder()
                .panier(panier)
                .session(session)
                .addedAt(LocalDateTime.now())
                .build();

        panierSessionRepository.save(panierSession);
        panier.setUpdatedAt(LocalDateTime.now());
        panierRepository.save(panier);

        return convertToDTO(panier);
    }

    @Transactional
    public PanierDTO removeSessionFromPanier(Long userId, Long sessionId) {
        Panier panier = panierRepository.findByUserIdAndStatus(userId, PanierStatus.EN_COURS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Panier introuvable"));

        PanierSession panierSession = panierSessionRepository.findByPanierIdAndSessionId(panier.getId(), sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session non trouvée dans le panier"));

        panierSessionRepository.delete(panierSession);
        panier.setUpdatedAt(LocalDateTime.now());
        panierRepository.save(panier);

        return convertToDTO(panier);
    }

    @Transactional
    public void clearPanier(Long userId) {
        Panier panier = panierRepository.findByUserIdAndStatus(userId, PanierStatus.EN_COURS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Panier introuvable"));

        panierSessionRepository.deleteByPanierId(panier.getId());
        panier.setUpdatedAt(LocalDateTime.now());
        panierRepository.save(panier);
    }

    private Panier createPanierForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        Panier panier = Panier.builder()
                .user(user)
                .status(PanierStatus.EN_COURS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return panierRepository.save(panier);
    }

    private void checkSessionAvailability(Session session) {
        long currentParticipants = participationRepository.countBySessionId(session.getId());
        if (currentParticipants >= session.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "La session " + session.getId() + " est complète (capacité: " + session.getCapacity() + ")");
        }
    }

    private boolean sessionsOverlap(Session session1, Session session2) {
        boolean datesOverlap = !session1.getEndDate().isBefore(session2.getStartDate()) 
                && !session2.getEndDate().isBefore(session1.getStartDate());
        
        if (!datesOverlap) {
            return false;
        }
        
        // Si même jour, vérifier les heures
        if (session1.getStartDate().equals(session2.getStartDate()) 
                && session1.getEndDate().equals(session2.getEndDate())) {
            return !session1.getEndTime().isBefore(session2.getStartTime()) 
                    && !session2.getEndTime().isBefore(session1.getStartTime());
        }
        
        return true;
    }

    private PanierDTO convertToDTO(Panier panier) {
        List<PanierSession> panierSessions = panierSessionRepository.findByPanierId(panier.getId());
        
        List<PanierSessionDTO> sessionDTOs = panierSessions.stream()
                .map(panierSession -> {
                    Session session = panierSession.getSession();
                    SessionDTO sessionDTO = new SessionDTO();
                    sessionDTO.setFormationId(session.getFormation().getId());
                    sessionDTO.setFormateurId(session.getFormateur().getId());
                    sessionDTO.setStartDate(session.getStartDate());
                    sessionDTO.setEndDate(session.getEndDate());
                    sessionDTO.setStartTime(session.getStartTime());
                    sessionDTO.setEndTime(session.getEndTime());
                    sessionDTO.setLocation(session.getLocation());
                    sessionDTO.setCapacity(session.getCapacity());
                    sessionDTO.setPrice(session.getPrice());
                    sessionDTO.setFormateurName(session.getFormateur().getFirstname() + " " + session.getFormateur().getLastname());
                    sessionDTO.setFormationImageUrl(session.getFormation().getImageUrl());
                    
                    return PanierSessionDTO.builder()
                            .id(panierSession.getId())
                            .sessionId(session.getId())
                            .session(sessionDTO)
                            .addedAt(panierSession.getAddedAt())
                            .build();
                })
                .collect(Collectors.toList());

        Double totalPrice = panierSessions.stream()
                .mapToDouble(panierSession -> panierSession.getSession().getPrice())
                .sum();

        return PanierDTO.builder()
                .id(panier.getId())
                .userId(panier.getUser().getId())
                .status(panier.getStatus())
                .sessions(sessionDTOs)
                .totalPrice(totalPrice)
                .createdAt(panier.getCreatedAt())
                .updatedAt(panier.getUpdatedAt())
                .build();
    }
}
