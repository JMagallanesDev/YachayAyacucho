package com.huamanga.tourism.reporte.repository;

import com.huamanga.tourism.reporte.domain.TipoIncidenteTraduccion;
import com.huamanga.tourism.reporte.domain.TipoIncidenteTraduccionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoIncidenteTraduccionRepository
        extends JpaRepository<TipoIncidenteTraduccion, TipoIncidenteTraduccionId> {
}
