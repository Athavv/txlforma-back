package mmi.osaas.txlforma.service;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.EmargementDTO;
import mmi.osaas.txlforma.enums.ParticipationStatus;
import mmi.osaas.txlforma.model.Emargement;
import mmi.osaas.txlforma.model.Participation;
import mmi.osaas.txlforma.model.Session;
import mmi.osaas.txlforma.model.User;
import mmi.osaas.txlforma.repository.EmargementRepository;
import mmi.osaas.txlforma.repository.ParticipationRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmargementService {

    private final EmargementRepository emargementRepository;
    private final ParticipationRepository participationRepository;
    @Lazy
    private final AttestationService attestationService;

    @Transactional
    public Emargement signParticipation(Long participationId, String signatureData, Long userId) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation introuvable"));
        if (!participation.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à signer cette participation");
        }
        if (emargementRepository.existsByParticipationId(participationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous avez déjà signé pour cette session");
        }
        Session session = participation.getSession();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
        ZonedDateTime sessionStart = ZonedDateTime.of(session.getStartDate(), session.getStartTime(), ZoneId.of("Europe/Paris"));
        ZonedDateTime sessionEnd = ZonedDateTime.of(session.getEndDate(), session.getEndTime(), ZoneId.of("Europe/Paris"));
        if (now.isBefore(sessionStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La session n'a pas encore commencé");
        }
        if (now.isAfter(sessionEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La session est terminée");
        }
        if (signatureData == null || signatureData.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La signature est requise");
        }
        Emargement emargement = Emargement.builder()
                .participation(participation)
                .signatureData(signatureData)
                .signedAt(ZonedDateTime.now(ZoneId.of("Europe/Paris")).toLocalDateTime())
                .present(true)
                .build();
        emargement = emargementRepository.save(emargement);
        participation.setStatus(ParticipationStatus.PRESENT);
        participation.setParticipationAt(ZonedDateTime.now(ZoneId.of("Europe/Paris")).toLocalDateTime());
        participationRepository.save(participation);
        try {
            attestationService.generateAttestation(participationId);
        } catch (Exception ignored) {
        }
        return emargement;
    }

    public List<EmargementDTO> getEmargementsBySession(Long sessionId) {
        return emargementRepository.findByParticipationSessionId(sessionId).stream()
                .map(this::toEmargementDTO)
                .collect(Collectors.toList());
    }

    public EmargementDTO getEmargementByParticipation(Long participationId, Long userId) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation introuvable"));
        if (!participation.getUser().getId().equals(userId) && !participation.getSession().getFormateur().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à voir cet émargement");
        }
        Emargement emargement = emargementRepository.findByParticipationId(participationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Émargement introuvable"));
        return toEmargementDTO(emargement);
    }

    private EmargementDTO toEmargementDTO(Emargement emargement) {
        Participation participation = emargement.getParticipation();
        User user = participation.getUser();
        return EmargementDTO.builder()
                .id(emargement.getId())
                .signatureData(emargement.getSignatureData())
                .signedAt(emargement.getSignedAt())
                .present(emargement.getPresent())
                .userId(user.getId())
                .userFirstname(user.getFirstname())
                .userLastname(user.getLastname())
                .userEmail(user.getEmail())
                .userImageUrl(user.getImageUrl())
                .participationId(participation.getId())
                .participationAt(participation.getParticipationAt())
                .build();
    }
}
