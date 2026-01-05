package mmi.osaas.txlforma.repository;

import mmi.osaas.txlforma.model.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    Optional<Paiement> findByPaymentIntentId(String paymentIntentId);
}

