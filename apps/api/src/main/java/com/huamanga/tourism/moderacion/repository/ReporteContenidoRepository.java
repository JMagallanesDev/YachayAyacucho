package com.huamanga.tourism.moderacion.repository;

import com.huamanga.tourism.moderacion.domain.ReporteContenido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReporteContenidoRepository extends JpaRepository<ReporteContenido, UUID> {

    /** Al tercer reporte distinto, el contenido pasa a EN_REVISION (RF-45). */
    long countByFotoId(UUID fotoId);

    long countByResenaId(UUID resenaId);

    boolean existsByUsuarioIdAndFotoId(UUID usuarioId, UUID fotoId);

    boolean existsByUsuarioIdAndResenaId(UUID usuarioId, UUID resenaId);
}
