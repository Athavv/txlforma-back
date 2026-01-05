package mmi.osaas.txlforma.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.SessionDTO;
import mmi.osaas.txlforma.dto.SessionResponseDTO;
import mmi.osaas.txlforma.model.Session;
import mmi.osaas.txlforma.model.User;
import mmi.osaas.txlforma.security.UserPrincipal;
import mmi.osaas.txlforma.service.SessionService;
import mmi.osaas.txlforma.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {
    
    private final SessionService sessionService;
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<List<Session>> getAllSessions(
            @RequestParam(required = false) Long formation,
            @RequestParam(required = false) Long formateur,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Session> sessions = sessionService.getAllSessions(formation, formateur, date);
        return ResponseEntity.ok(sessions);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Session> getSessionById(@PathVariable Long id) {
        Session session = sessionService.getSessionById(id);
        return ResponseEntity.ok(session);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Session> createSession(
            @Valid @RequestBody SessionDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(sessionService.createSession(dto, principal.getId()));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Session> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(sessionService.updateSession(id, dto, principal.getId()));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok("Session supprimée avec succès");
    }
    
    
    @GetMapping("/formateurs")
    public ResponseEntity<List<User>> getAvailableFormateurs() {
        List<User> formateurs = userService.getFormateurs();
        return ResponseEntity.ok(formateurs);
    }
    
    @GetMapping("/formateur/{formateurId}")
    public ResponseEntity<List<Session>> getSessionsByFormateur(@PathVariable Long formateurId) {
        List<Session> sessions = sessionService.getSessionsByFormateur(formateurId);
        return ResponseEntity.ok(sessions);
    }
    
    @GetMapping("/me")
    @PreAuthorize("hasRole('FORMATEUR')")
    public ResponseEntity<List<SessionResponseDTO>> getMySessions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(sessionService.getMySessions(principal.getId()));
    }
}

