package com.huamanga.tourism.reporte.repository;

import com.huamanga.tourism.reporte.domain.FotoReporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FotoReporteRepository extends JpaRepository<FotoReporte, UUID> {

    List<FotoReporte> findByReporteIdOrderByOrdenAsc(UUID reporteId);

    long countByReporteId(UUID reporteId);
}
