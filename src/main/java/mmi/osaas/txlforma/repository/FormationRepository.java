package mmi.osaas.txlforma.repository;

import mmi.osaas.txlforma.model.Formation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormationRepository extends JpaRepository<Formation, Long> {
    List<Formation> findByCategoryId(Long categoryId);
}

