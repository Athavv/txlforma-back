package mmi.osaas.txlforma.controller;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.NoteDTO;
import mmi.osaas.txlforma.security.UserPrincipal;
import mmi.osaas.txlforma.service.NoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    @PreAuthorize("hasRole('FORMATEUR')")
    public ResponseEntity<NoteDTO> createNote(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long participationId = getLongFromRequest(request, "participationId");
        Double noteValue = getDoubleFromRequest(request, "note");

        if (participationId == null || noteValue == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "participationId et note requis");
        }

        return ResponseEntity.ok(noteService.createNote(participationId, noteValue, principal.getId()));
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasRole('FORMATEUR')")
    public ResponseEntity<NoteDTO> updateNote(
            @PathVariable Long noteId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Double noteValue = getDoubleFromRequest(request, "note");
        if (noteValue == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "note requis");
        }

        return ResponseEntity.ok(noteService.updateNote(noteId, noteValue, principal.getId()));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'ADMIN')")
    public ResponseEntity<List<NoteDTO>> getNotesBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(noteService.getNotesBySession(sessionId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<NoteDTO>> getMyNotes(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.getMyNotes(principal.getId()));
    }

    @GetMapping("/participation/{participationId}")
    public ResponseEntity<NoteDTO> getNoteByParticipation(
            @PathVariable Long participationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noteService.getNoteByParticipation(participationId, principal.getId()));
    }

    private Long getLongFromRequest(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Double getDoubleFromRequest(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble((String) value); } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
