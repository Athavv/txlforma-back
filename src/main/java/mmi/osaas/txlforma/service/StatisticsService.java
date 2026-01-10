package mmi.osaas.txlforma.service;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.enums.AttestationType;
import mmi.osaas.txlforma.enums.PaiementStatus;
import mmi.osaas.txlforma.model.Attestation;
import mmi.osaas.txlforma.model.Emargement;
import mmi.osaas.txlforma.model.Note;
import mmi.osaas.txlforma.model.Paiement;
import mmi.osaas.txlforma.model.Participation;
import mmi.osaas.txlforma.model.Session;
import mmi.osaas.txlforma.model.User;
import mmi.osaas.txlforma.repository.AttestationRepository;
import mmi.osaas.txlforma.repository.EmargementRepository;
import mmi.osaas.txlforma.repository.NoteRepository;
import mmi.osaas.txlforma.repository.PaiementRepository;
import mmi.osaas.txlforma.repository.ParticipationRepository;
import mmi.osaas.txlforma.repository.SessionRepository;
import mmi.osaas.txlforma.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ParticipationRepository participationRepository;
    private final PaiementRepository paiementRepository;
    private final SessionRepository sessionRepository;
    private final AttestationRepository attestationRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final EmargementRepository emargementRepository;

    public Map<String, Object> getGlobalStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();

        List<Participation> participations = participationRepository.findAll();
        if (startDate != null && endDate != null) {
            participations = participations.stream()
                    .filter(participation -> {
                        LocalDate sessionDate = participation.getSession().getStartDate();
                        return !sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate);
                    })
                    .collect(Collectors.toList());
        }

        long totalParticipants = participations.size();
        long totalSessions = participations.stream()
                .map(participation -> participation.getSession().getId())
                .distinct()
                .count();

        List<Attestation> successAttestations = attestationRepository.findAll().stream()
                .filter(attestation -> attestation.getType() == AttestationType.SUCCES)
                .filter(attestation -> {
                    if (startDate == null || endDate == null) return true;
                    LocalDate sessionDate = attestation.getParticipation().getSession().getStartDate();
                    return !sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        long totalSuccess = successAttestations.size();
        double successRate = totalParticipants > 0 ? (double) totalSuccess / totalParticipants * 100 : 0;

        List<Paiement> successfulPayments = paiementRepository.findAll().stream()
                .filter(paiement -> paiement.getStatus() == PaiementStatus.SUCCEEDED)
                .filter(paiement -> {
                    if (startDate == null || endDate == null) return true;
                    LocalDateTime paymentDate = paiement.getCreatedAt();
                    LocalDate paymentLocalDate = paymentDate.toLocalDate();
                    return !paymentLocalDate.isBefore(startDate) && !paymentLocalDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        double totalRevenue = successfulPayments.stream()
                .mapToDouble(Paiement::getAmount)
                .sum();

        stats.put("totalParticipants", totalParticipants);
        stats.put("totalSessions", totalSessions);
        stats.put("totalSuccess", totalSuccess);
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
        stats.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
        stats.put("period", Map.of(
                "startDate", startDate != null ? startDate.toString() : "all",
                "endDate", endDate != null ? endDate.toString() : "all"
        ));

        return stats;
    }

    public Map<String, Object> getFormateurStatistics(Long formateurId) {
        User formateur = userRepository.findById(formateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formateur introuvable"));

        if (formateur.getRole() != mmi.osaas.txlforma.enums.Role.FORMATEUR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'utilisateur n'est pas un formateur");
        }

        List<Session> sessions = sessionRepository.findByFormateurId(formateurId);
        long totalSessions = sessions.size();

        List<Participation> participations = participationRepository.findAll().stream()
                .filter(participation -> participation.getSession().getFormateur().getId().equals(formateurId))
                .collect(Collectors.toList());

        long totalParticipants = participations.size();

        long totalHours = sessions.stream()
                .mapToLong(session -> {
                    long days = ChronoUnit.DAYS.between(session.getStartDate(), session.getEndDate()) + 1;
                    long hoursPerDay = ChronoUnit.HOURS.between(session.getStartTime(), session.getEndTime());
                    return days * hoursPerDay;
                })
                .sum();

        List<Note> notes = noteRepository.findAll().stream()
                .filter(note -> note.getGivenBy().getId().equals(formateurId))
                .collect(Collectors.toList());

        long totalNotes = notes.size();
        double averageNote = notes.stream()
                .mapToDouble(Note::getNote)
                .average()
                .orElse(0.0);

        List<Attestation> successAttestations = attestationRepository.findAll().stream()
                .filter(attestation -> attestation.getType() == AttestationType.SUCCES)
                .filter(attestation -> attestation.getParticipation().getSession().getFormateur().getId().equals(formateurId))
                .collect(Collectors.toList());

        long totalSuccess = successAttestations.size();
        double successRate = totalParticipants > 0 ? (double) totalSuccess / totalParticipants * 100 : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("formateurId", formateurId);
        stats.put("formateurName", formateur.getFirstname() + " " + formateur.getLastname());
        stats.put("totalSessions", totalSessions);
        stats.put("totalParticipants", totalParticipants);
        stats.put("totalHours", totalHours);
        stats.put("totalNotes", totalNotes);
        stats.put("averageNote", Math.round(averageNote * 100.0) / 100.0);
        stats.put("totalSuccess", totalSuccess);
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);

        return stats;
    }

    public List<Map<String, Object>> getAllFormateursStatistics() {
        List<User> formateurs = userRepository.findAll().stream()
                .filter(user -> user.getRole() == mmi.osaas.txlforma.enums.Role.FORMATEUR)
                .collect(Collectors.toList());

        return formateurs.stream()
                .map(formateur -> getFormateurStatistics(formateur.getId()))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getSessionDetails(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session introuvable"));

        List<Participation> participations = participationRepository.findBySessionId(sessionId);

        List<Map<String, Object>> participantsDetails = participations.stream()
                .map(participation -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("participationId", participation.getId());
                    details.put("userId", participation.getUser().getId());
                    details.put("userName", participation.getUser().getFirstname() + " " + participation.getUser().getLastname());
                    details.put("userEmail", participation.getUser().getEmail());
                    details.put("userImageUrl", participation.getUser().getImageUrl());
                    details.put("status", "PAYE");
                    Paiement paiement = participation.getPaiement();
                    if (paiement != null) {
                        if (paiement.getPaymentMethod() != null) {
                            details.put("paymentMethod", paiement.getPaymentMethod());
                        } else {
                            details.put("paymentMethod", "carte");
                        }
                        if (paiement.getCreatedAt() != null) {
                            details.put("paymentDate", paiement.getCreatedAt());
                        }
                    } else {
                        details.put("paymentMethod", "carte");
                    }
                    
                    Emargement emargement = emargementRepository.findByParticipationId(participation.getId()).orElse(null);
                    details.put("hasSigned", emargement != null);
                    if (emargement != null) {
                        details.put("signedAt", emargement.getSignedAt());
                    }

                    Note note = noteRepository.findByParticipationId(participation.getId()).orElse(null);
                    details.put("hasNote", note != null);
                    if (note != null) {
                        details.put("note", note.getNote());
                        details.put("noteLocked", note.getLocked());
                    }

                    List<Attestation> attestations = attestationRepository.findByParticipationId(participation.getId());
                    List<Map<String, Object>> attestationsList = attestations.stream()
                            .map(attestation -> {
                                Map<String, Object> attMap = new HashMap<>();
                                attMap.put("id", attestation.getId());
                                attMap.put("type", attestation.getType().toString());
                                attMap.put("generatedAt", attestation.getGeneratedAt());
                                return attMap;
                            })
                            .collect(Collectors.toList());
                    details.put("attestations", attestationsList);

                    return details;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("formationTitle", session.getFormation().getTitle());
        result.put("formateurName", session.getFormateur().getFirstname() + " " + session.getFormateur().getLastname());
        result.put("startDate", session.getStartDate());
        result.put("endDate", session.getEndDate());
        result.put("location", session.getLocation());
        result.put("capacity", session.getCapacity());
        result.put("totalParticipants", participations.size());
        result.put("participants", participantsDetails);

        return result;
    }
}

