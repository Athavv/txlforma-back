package mmi.osaas.txlforma.repository;

import mmi.osaas.txlforma.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    List<Session> findByFormationId(Long formationId);
    
    @Query("SELECT session FROM Session session " +
           "JOIN FETCH session.formation " +
           "JOIN FETCH session.formateur " +
           "WHERE session.formateur.id = :formateurId")
    List<Session> findByFormateurId(@Param("formateurId") Long formateurId);
    
    List<Session> findByStartDate(LocalDate startDate);
    
    List<Session> findByFormationIdAndFormateurId(Long formationId, Long formateurId);
    
    @Query("SELECT session FROM Session session " +
           "WHERE session.formation.id = :formationId " +
           "AND (:formateurId IS NULL OR session.formateur.id = :formateurId) " +
           "AND (:startDate IS NULL OR session.startDate = :startDate)")
    List<Session> findWithFilters(@Param("formationId") Long formationId,
                                   @Param("formateurId") Long formateurId,
                                   @Param("startDate") LocalDate startDate);
    
    @Query("SELECT session FROM Session session " +
           "WHERE session.formateur.id = :formateurId " +
           "AND ((session.startDate < :endDate OR (session.startDate = :endDate AND session.startTime < :endTime)) " +
           "AND (session.endDate > :startDate OR (session.endDate = :startDate AND session.endTime > :startTime))) " +
           "AND (:excludeSessionId IS NULL OR session.id != :excludeSessionId)")
    List<Session> findOverlappingSessions(@Param("formateurId") Long formateurId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("startTime") LocalTime startTime,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("endTime") LocalTime endTime,
                                          @Param("excludeSessionId") Long excludeSessionId);
}

