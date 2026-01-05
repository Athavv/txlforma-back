package mmi.osaas.txlforma.repository;

import mmi.osaas.txlforma.model.PanierSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PanierSessionRepository extends JpaRepository<PanierSession, Long> {
    List<PanierSession> findByPanierId(Long panierId);
    List<PanierSession> findBySessionId(Long sessionId);
    Optional<PanierSession> findByPanierIdAndSessionId(Long panierId, Long sessionId);
    boolean existsByPanierIdAndSessionId(Long panierId, Long sessionId);
    void deleteByPanierId(Long panierId);
    void deleteByPanierIdAndSessionId(Long panierId, Long sessionId);
    
    @Query("SELECT panierSession FROM PanierSession panierSession " +
           "WHERE panierSession.panier.user.id = :userId " +
           "AND panierSession.panier.status = 'EN_COURS'")
    List<PanierSession> findActivePanierSessionsByUserId(@Param("userId") Long userId);
}
