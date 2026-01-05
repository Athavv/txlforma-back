package mmi.osaas.txlforma.repository;

import mmi.osaas.txlforma.enums.PanierStatus;
import mmi.osaas.txlforma.model.Panier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PanierRepository extends JpaRepository<Panier, Long> {
    Optional<Panier> findByUserIdAndStatus(Long userId, PanierStatus status);
}
