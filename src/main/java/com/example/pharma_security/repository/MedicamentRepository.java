package com.example.pharma_security.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.example.pharma_security.model.Medicament;

public interface MedicamentRepository extends JpaRepository<Medicament, Long> {
    List<Medicament> findByActifTrueOrderByNomAsc();
    List<Medicament> findByActifTrueAndQuantiteGreaterThanOrderByNomAsc(int quantite);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medicament m where m.id = :id")
    Optional<Medicament> findLockedById(@Param("id") Long id);
}
