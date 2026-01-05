package mmi.osaas.txlforma.repository;

import mmi.osaas.txlforma.enums.AttestationType;
import mmi.osaas.txlforma.model.Attestation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttestationRepository extends JpaRepository<Attestation, Long> {
    List<Attestation> findByParticipationUserId(Long userId);
    List<Attestation> findByParticipationId(Long participationId);
    Optional<Attestation> findByParticipationIdAndType(Long participationId, AttestationType type);
}

