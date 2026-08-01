package com.huamanga.tourism.reporte.repository;

import com.huamanga.tourism.reporte.domain.TipoIncidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TipoIncidenteRepository extends JpaRepository<TipoIncidente, UUID> {

    Optional<TipoIncidente> findByCodigo(String codigo);

    @Query("SELECT DISTINCT t FROM TipoIncidente t LEFT JOIN FETCH t.traducciones")
    List<TipoIncidente> findAllConTraducciones();
}
