package mmi.osaas.txlforma.service;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.ParticipationDTO;
import mmi.osaas.txlforma.dto.SessionParticipantDTO;
import mmi.osaas.txlforma.model.Formation;
import mmi.osaas.txlforma.model.Note;
import mmi.osaas.txlforma.model.Paiement;
import mmi.osaas.txlforma.model.Participation;
import mmi.osaas.txlforma.model.Session;
import mmi.osaas.txlforma.model.User;
import mmi.osaas.txlforma.repository.NoteRepository;
import mmi.osaas.txlforma.repository.ParticipationRepository;
import mmi.osaas.txlforma.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final SessionRepository sessionRepository;
    private final NoteRepository noteRepository;

    public List<ParticipationDTO> getMyParticipations(Long userId) {
        return participationRepository.findByUserId(userId).stream()
                .map(this::toParticipationDTO)
                .collect(Collectors.toList());
    }

    public List<SessionParticipantDTO> getSessionParticipants(Long sessionId, Long formateurId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session introuvable"));

        if (formateurId != null && !session.getFormateur().getId().equals(formateurId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n'êtes pas autorisé à voir les participants de cette session");
        }

        return participationRepository.findBySessionId(sessionId).stream()
                .map(this::toSessionParticipantDTO)
                .collect(Collectors.toList());
    }

    public long getSessionParticipationsCount(Long sessionId) {
        return participationRepository.countBySessionId(sessionId);
    }

    private ParticipationDTO toParticipationDTO(Participation participation) {
        Session session = participation.getSession();
        Formation formation = session.getFormation();
        User formateur = session.getFormateur();
        Double noteValue = noteRepository.findByParticipationId(participation.getId())
                .map(Note::getNote)
                .orElse(null);
        return ParticipationDTO.builder()
                .id(participation.getId())
                .status(participation.getStatus())
                .participationAt(participation.getParticipationAt())
                .createdAt(participation.getCreatedAt())
                .note(noteValue)
                .sessionId(session.getId())
                .startDate(session.getStartDate())
                .endDate(session.getEndDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .location(session.getLocation())
                .price(session.getPrice())
                .formationId(formation.getId())
                .formationTitle(formation.getTitle())
                .formationDescription(formation.getDescription())
                .formationImageUrl(formation.getImageUrl())
                .categoryName(formation.getCategory() != null ? formation.getCategory().getName() : null)
                .formateurId(formateur.getId())
                .formateurFirstname(formateur.getFirstname())
                .formateurLastname(formateur.getLastname())
                .formateurImageUrl(formateur.getImageUrl())
                .paiementStatus(participation.getPaiement().getStatus())

                .build();
    }

    private SessionParticipantDTO toSessionParticipantDTO(Participation participation) {
        User user = participation.getUser();
        Paiement paiement = participation.getPaiement();

        return SessionParticipantDTO.builder()
                .id(participation.getId())
                .status(participation.getStatus())
                .participationAt(participation.getParticipationAt())
                .createdAt(participation.getCreatedAt())
                .userId(user.getId())
                .userFirstname(user.getFirstname())
                .userLastname(user.getLastname())
                .userEmail(user.getEmail())
                .userImageUrl(user.getImageUrl())
                .paiementStatus(paiement.getStatus())
                .paiementAmount(paiement.getAmount())
                .paiementCurrency(paiement.getCurrency())
                .build();
    }
}
