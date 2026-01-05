package mmi.osaas.txlforma.controller;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.security.UserPrincipal;
import mmi.osaas.txlforma.service.PaiementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaiementController {

    private final PaiementService paiementService;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @RequestParam(required = false) Long panierId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paiementService.createCheckoutSession(principal.getId(), panierId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(HttpServletRequest request) throws java.io.IOException {
        String payload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        String signature = request.getHeader("Stripe-Signature");
        paiementService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync-checkout-session")
    public ResponseEntity<Map<String, String>> syncCheckoutSession(
            @RequestParam String sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        paiementService.syncCheckoutSession(sessionId, principal.getId());
        Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Paiement synchronisé avec succès");
        response.put("success", "true");
        return ResponseEntity.ok(response);
    }
}
