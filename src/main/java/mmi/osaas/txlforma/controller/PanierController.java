package mmi.osaas.txlforma.controller;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.PanierDTO;
import mmi.osaas.txlforma.security.UserPrincipal;
import mmi.osaas.txlforma.service.PanierService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panier")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PanierController {

    private final PanierService panierService;

    @GetMapping
    public ResponseEntity<PanierDTO> getPanier(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(panierService.getPanierByUserId(principal.getId()));
    }

    @PostMapping("/sessions/{sessionId}")
    public ResponseEntity<PanierDTO> addSessionToPanier(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(panierService.addSessionToPanier(principal.getId(), sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<PanierDTO> removeSessionFromPanier(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(panierService.removeSessionFromPanier(principal.getId(), sessionId));
    }

    @DeleteMapping
    public ResponseEntity<?> clearPanier(@AuthenticationPrincipal UserPrincipal principal) {
        panierService.clearPanier(principal.getId());
        return ResponseEntity.ok("Panier vidé avec succès");
    }
}
