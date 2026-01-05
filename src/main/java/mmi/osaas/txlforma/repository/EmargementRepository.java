package mmi.osaas.txlforma.repository;

import mmi.osaas.txlforma.model.Emargement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmargementRepository extends JpaRepository<Emargement, Long> {
    
    @Query("SELECT emargement FROM Emargement emargement " +
           "JOIN FETCH emargement.participation participation " +
           "JOIN FETCH participation.user " +
           "WHERE participation.id = :participationId")
    Optional<Emargement> findByParticipationId(@Param("participationId") Long participationId);
    
    @Query("SELECT emargement FROM Emargement emargement " +
           "JOIN FETCH emargement.participation participation " +
           "JOIN FETCH participation.user " +
           "WHERE participation.session.id = :sessionId")
    List<Emargement> findByParticipationSessionId(@Param("sessionId") Long sessionId);
    
    boolean existsByParticipationId(Long participationId);
}

