package mmi.osaas.txlforma.repository;

import mmi.osaas.txlforma.enums.ParticipationStatus;
import mmi.osaas.txlforma.model.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    
    @Query("SELECT participation FROM Participation participation " +
           "JOIN FETCH participation.session session " +
           "JOIN FETCH session.formation formation " +
           "JOIN FETCH session.formateur " +
           "JOIN FETCH participation.paiement " +
           "WHERE participation.user.id = :userId")
    List<Participation> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT participation FROM Participation participation " +
           "JOIN FETCH participation.user " +
           "JOIN FETCH participation.paiement " +
           "WHERE participation.session.id = :sessionId")
    List<Participation> findBySessionId(@Param("sessionId") Long sessionId);
    boolean existsByUserIdAndSessionId(Long userId, Long sessionId);
    
    @Query("SELECT COUNT(participation) FROM Participation participation " +
           "WHERE participation.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT participation FROM Participation participation " +
           "WHERE participation.user.id = :userId " +
           "AND participation.session.formation.id = :formationId " +
           "AND participation.status = :status")
    List<Participation> findByUserIdAndFormationIdAndStatus(@Param("userId") Long userId, 
                                                             @Param("formationId") Long formationId, 
                                                             @Param("status") ParticipationStatus status);
    
    @Query("SELECT participation FROM Participation participation " +
           "WHERE participation.user.id = :userId " +
           "AND participation.session.formation.id = :formationId " +
           "AND participation.status = 'VALIDE'")
    List<Participation> findValidByUserIdAndFormationId(@Param("userId") Long userId, 
                                                         @Param("formationId") Long formationId);
    
    Optional<Participation> findByUserIdAndSessionId(Long userId, Long sessionId);
    
    @Query("SELECT COUNT(participation) > 0 FROM Participation participation " +
           "WHERE participation.user.id = :userId " +
           "AND participation.paiement.id = :paiementId")
    boolean existsByUserIdAndPaiementId(@Param("userId") Long userId, 
                                        @Param("paiementId") Long paiementId);
    
    List<Participation> findByPaiementId(Long paiementId);
}

