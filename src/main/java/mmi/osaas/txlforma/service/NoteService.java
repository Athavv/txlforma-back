package mmi.osaas.txlforma.service;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.NoteDTO;
import mmi.osaas.txlforma.enums.ParticipationStatus;
import mmi.osaas.txlforma.exception.NoteLockedException;
import mmi.osaas.txlforma.model.Note;
import mmi.osaas.txlforma.model.Participation;
import mmi.osaas.txlforma.model.Session;
import mmi.osaas.txlforma.model.User;
import mmi.osaas.txlforma.repository.NoteRepository;
import mmi.osaas.txlforma.repository.ParticipationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final ParticipationRepository participationRepository;

    @Transactional
    public NoteDTO createNote(Long participationId, Double noteValue, Long formateurId) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation introuvable"));

        if (participation.getStatus() != ParticipationStatus.PRESENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seuls les participants présents peuvent être notés");
        }

        Session session = participation.getSession();
        if (!session.getFormateur().getId().equals(formateurId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas le formateur de cette session");
        }

        if (noteValue < 0 || noteValue > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note doit être entre 0 et 20");
        }

        if (noteRepository.findByParticipationId(participationId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une note existe déjà pour cette participation");
        }

        Note note = Note.builder()
                .participation(participation)
                .givenBy(session.getFormateur())
                .note(noteValue)
                .createdAt(LocalDateTime.now())
                .locked(false)
                .build();

        return toNoteDTO(noteRepository.save(note));
    }

    @Transactional
    public NoteDTO updateNote(Long noteId, Double noteValue, Long formateurId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note introuvable"));

        if (!note.getGivenBy().getId().equals(formateurId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à modifier cette note");
        }

        if (note.getLocked()) {
            throw new NoteLockedException("Cette note est verrouillée et ne peut plus être modifiée");
        }

        if (isNoteLocked(note)) {
            note.setLocked(true);
            noteRepository.save(note);
            throw new NoteLockedException("Le délai de modification (14 jours) est dépassé");
        }

        if (noteValue < 0 || noteValue > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note doit être entre 0 et 20");
        }

        note.setNote(noteValue);
        note.setModifiedAt(LocalDateTime.now());
        return toNoteDTO(noteRepository.save(note));
    }

    public List<NoteDTO> getNotesBySession(Long sessionId) {
        return noteRepository.findByParticipationSessionId(sessionId).stream()
                .map(this::toNoteDTO)
                .collect(Collectors.toList());
    }

    public List<NoteDTO> getMyNotes(Long userId) {
        return noteRepository.findByParticipationUserId(userId).stream()
                .map(this::toNoteDTO)
                .collect(Collectors.toList());
    }

    public NoteDTO getNoteByParticipation(Long participationId, Long userId) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation introuvable"));

        if (!participation.getUser().getId().equals(userId) && 
            !participation.getSession().getFormateur().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à voir cette note");
        }

        Note note = noteRepository.findByParticipationId(participationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note introuvable"));
        
        return toNoteDTO(note);
    }

    private NoteDTO toNoteDTO(Note note) {
        Participation participation = note.getParticipation();
        User user = participation.getUser();
        User formateur = note.getGivenBy();
        
        return NoteDTO.builder()
                .id(note.getId())
                .note(note.getNote())
                .locked(note.getLocked())
                .createdAt(note.getCreatedAt())
                .modifiedAt(note.getModifiedAt())
                .participationId(participation.getId())
                .participationStatus(participation.getStatus())
                .userId(user.getId())
                .userFirstname(user.getFirstname())
                .userLastname(user.getLastname())
                .userEmail(user.getEmail())
                .formateurId(formateur.getId())
                .formateurName(formateur.getFirstname() + " " + formateur.getLastname())
                .build();
    }

    private boolean isNoteLocked(Note note) {
        LocalDate deadline = note.getParticipation().getSession().getEndDate().plusDays(14);
        return LocalDate.now().isAfter(deadline);
    }

    @Transactional
    public void lockNotesPastDeadline() {
        List<Note> unlockableNotes = noteRepository.findAll().stream()
                .filter(note -> !note.getLocked() && isNoteLocked(note))
                .toList();
        
        for (Note note : unlockableNotes) {
            note.setLocked(true);
            noteRepository.save(note);

            if (note.getNote() >= 10) {
                Participation participation = note.getParticipation();
                if (participation.getStatus() != ParticipationStatus.VALIDE) {
                    participation.setStatus(ParticipationStatus.VALIDE);
                    participationRepository.save(participation);
                }
            }
        }
    }
}

