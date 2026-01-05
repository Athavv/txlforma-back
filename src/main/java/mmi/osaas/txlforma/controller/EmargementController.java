package mmi.osaas.txlforma.controller;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.EmargementDTO;
import mmi.osaas.txlforma.model.Emargement;
import mmi.osaas.txlforma.security.UserPrincipal;
import mmi.osaas.txlforma.service.EmargementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emargements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmargementController {

    private final EmargementService emargementService;

    @PostMapping("/sign")
    public ResponseEntity<Emargement> signParticipation(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long participationId = getLongFromRequest(request, "participationId");
        String signatureData = (String) request.get("signatureData");

        if (participationId == null || signatureData == null || signatureData.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "participationId et signatureData requis");
        }

        return ResponseEntity.ok(emargementService.signParticipation(
                participationId, signatureData, principal.getId()));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'ADMIN')")
    public ResponseEntity<List<EmargementDTO>> getEmargementsBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(emargementService.getEmargementsBySession(sessionId));
    }

    @GetMapping("/participation/{participationId}")
    public ResponseEntity<EmargementDTO> getEmargementByParticipation(
            @PathVariable Long participationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(emargementService.getEmargementByParticipation(
                participationId, principal.getId()));
    }

    private Long getLongFromRequest(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}

