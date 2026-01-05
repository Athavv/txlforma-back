package mmi.osaas.txlforma.service;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.SessionDTO;
import mmi.osaas.txlforma.dto.SessionResponseDTO;
import mmi.osaas.txlforma.enums.Role;
import mmi.osaas.txlforma.model.Formation;
import mmi.osaas.txlforma.model.Participation;
import mmi.osaas.txlforma.model.Session;
import mmi.osaas.txlforma.model.User;
import mmi.osaas.txlforma.repository.FormationRepository;
import mmi.osaas.txlforma.repository.PanierSessionRepository;
import mmi.osaas.txlforma.repository.ParticipationRepository;
import mmi.osaas.txlforma.repository.SessionRepository;
import mmi.osaas.txlforma.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final SessionRepository sessionRepository;
    private final FormationRepository formationRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final PanierSessionRepository panierSessionRepository;
    
    public List<Session> getAllSessions(Long formationId, Long formateurId, LocalDate startDate) {
        if (formationId != null || formateurId != null || startDate != null) {
            return sessionRepository.findWithFilters(formationId, formateurId, startDate);
        }
        return sessionRepository.findAll();
    }
    
    public Session getSessionById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session introuvable"));
    }
    
    @Transactional
    public Session createSession(SessionDTO dto, Long createdById) {
        validateSessionData(dto);
        
        Formation formation = formationRepository.findById(dto.getFormationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formation introuvable"));
        
        User formateur = userRepository.findById(dto.getFormateurId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formateur introuvable"));
        
        if (formateur.getRole() != Role.FORMATEUR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'utilisateur sélectionné n'est pas un formateur");
        }
        
        checkFormateurAvailability(dto.getFormateurId(), dto.getStartDate(), dto.getStartTime(),
                dto.getEndDate(), dto.getEndTime(), null);
        
        User createdBy = userRepository.findById(createdById)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur créateur introuvable"));
        
        Session session = Session.builder()
                .formation(formation)
                .formateur(formateur)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .location(dto.getLocation())
                .capacity(dto.getCapacity())
                .price(dto.getPrice())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();
        
        return sessionRepository.save(session);
    }
    
    @Transactional
    public Session updateSession(Long id, SessionDTO dto, Long updatedById) {
        Session existing = getSessionById(id);
        validateSessionData(dto);
        
        Formation formation = formationRepository.findById(dto.getFormationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formation introuvable"));
        
        User formateur = userRepository.findById(dto.getFormateurId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formateur introuvable"));
        
        if (formateur.getRole() != Role.FORMATEUR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'utilisateur sélectionné n'est pas un formateur");
        }
        
        checkFormateurAvailability(dto.getFormateurId(), dto.getStartDate(), dto.getStartTime(),
                dto.getEndDate(), dto.getEndTime(), id);
        
        existing.setFormation(formation);
        existing.setFormateur(formateur);
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setStartTime(dto.getStartTime());
        existing.setEndTime(dto.getEndTime());
        existing.setLocation(dto.getLocation());
        existing.setCapacity(dto.getCapacity());
        existing.setPrice(dto.getPrice());
        
        return sessionRepository.save(existing);
    }
    
    @Transactional
    public void deleteSession(Long id) {
        getSessionById(id);
        
        List<Participation> participations = participationRepository.findBySessionId(id);
        if (!participations.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, 
                "Impossible de supprimer cette session car " + participations.size() + " utilisateur(s) sont déjà inscrits. Veuillez d'abord annuler les inscriptions."
            );
        }
        
        panierSessionRepository.deleteAll(panierSessionRepository.findBySessionId(id));
        sessionRepository.deleteById(id);
    }
    
    public List<Session> getSessionsByFormateur(Long formateurId) {
        return sessionRepository.findByFormateurId(formateurId);
    }
    
    public List<SessionResponseDTO> getMySessions(Long formateurId) {
        List<Session> sessions = sessionRepository.findByFormateurId(formateurId);
        return sessions.stream()
                .map(this::toSessionResponseDTO)
                .collect(Collectors.toList());
    }
    
    private SessionResponseDTO toSessionResponseDTO(Session session) {
        Formation formation = session.getFormation();
        User formateur = session.getFormateur();
        
        return SessionResponseDTO.builder()
                .id(session.getId())
                .startDate(session.getStartDate())
                .endDate(session.getEndDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .location(session.getLocation())
                .capacity(session.getCapacity())
                .price(session.getPrice())
                .createdAt(session.getCreatedAt())
                .formationId(formation.getId())
                .formationTitle(formation.getTitle())
                .formationImageUrl(formation.getImageUrl())
                .formateurId(formateur.getId())
                .formateurName(formateur.getFirstname() + " " + formateur.getLastname())
                .build();
    }
    
    private void validateSessionData(SessionDTO dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "La date de fin doit être postérieure ou égale à la date de début");
        }
        
        if (dto.getEndDate().equals(dto.getStartDate()) && 
            dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "L'heure de fin doit être postérieure à l'heure de début");
        }
    }
    
    private void checkFormateurAvailability(Long formateurId, LocalDate startDate, LocalTime startTime,
                                            LocalDate endDate, LocalTime endTime, Long excludeSessionId) {
        List<Session> overlappingSessions = sessionRepository.findOverlappingSessions(
                formateurId, startDate, startTime, endDate, endTime, excludeSessionId);
        
        if (!overlappingSessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Le formateur a déjà une session qui se chevauche avec cette période");
        }
    }
}

