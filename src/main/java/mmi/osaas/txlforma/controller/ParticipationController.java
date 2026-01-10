package mmi.osaas.txlforma.controller;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.ParticipationDTO;
import mmi.osaas.txlforma.dto.SessionParticipantDTO;
import mmi.osaas.txlforma.enums.Role;
import mmi.osaas.txlforma.security.UserPrincipal;
import mmi.osaas.txlforma.service.ParticipationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> getSessionParticipants(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        if (principal != null && 
            (principal.getUser().getRole() == Role.ADMIN || 
             principal.getUser().getRole() == Role.FORMATEUR)) {
            Long formateurId = principal.getUser().getRole() == Role.FORMATEUR ? principal.getId() : null;
            return ResponseEntity.ok(participationService.getSessionParticipants(sessionId, formateurId));
        }
        
        long count = participationService.getSessionParticipationsCount(sessionId);
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
}