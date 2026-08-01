package com.huamanga.tourism.insignia.repository;

import com.huamanga.tourism.insignia.domain.Insignia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsigniaRepository extends JpaRepository<Insignia, UUID> {

    Optional<Insignia> findByCodigo(String codigo);

    @Query("SELECT DISTINCT i FROM Insignia i LEFT JOIN FETCH i.traducciones ORDER BY i.orden")
    List<Insignia> findAllConTraducciones();
}
