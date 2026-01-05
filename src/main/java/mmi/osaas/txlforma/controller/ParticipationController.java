package mmi.osaas.txlforma.controller;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.ParticipationDTO;
import mmi.osaas.txlforma.dto.SessionParticipantDTO;
import mmi.osaas.txlforma.security.UserPrincipal;
import mmi.osaas.txlforma.service.ParticipationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ParticipationController {

    private final ParticipationService participationService;

    @GetMapping("/me")
    public ResponseEntity<List<ParticipationDTO>> getMyParticipations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(participationService.getMyParticipations(principal.getId()));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'ADMIN')")
    public ResponseEntity<List<SessionParticipantDTO>> getSessionParticipants(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long formateurId = principal.getUser().getRole().name().equals("FORMATEUR") ? principal.getId() : null;
        return ResponseEntity.ok(participationService.getSessionParticipants(sessionId, formateurId));
    }
}

