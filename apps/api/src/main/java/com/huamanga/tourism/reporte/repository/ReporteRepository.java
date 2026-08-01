package com.huamanga.tourism.reporte.repository;

import com.huamanga.tourism.reporte.domain.EstadoReporte;
import com.huamanga.tourism.reporte.domain.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReporteRepository extends JpaRepository<Reporte, UUID> {

    /** Bandeja de moderacion (RF-76). Usa idx_reporte_estado. */
    Page<Reporte> findByEstadoOrderByCreatedAtDesc(EstadoReporte estado, Pageable pageable);

    /**
     * Incidentes aprobados dentro del area visible del mapa (RF-74).
     * Usa el indice GIST idx_reporte_ubicacion.
     */
    @Query(value = """
            SELECT * FROM reporte r
            WHERE r.deleted_at IS NULL
              AND r.estado = 'APROBADO'
              AND ST_Within(r.ubicacion,
                            ST_MakeEnvelope(:oeste, :sur, :este, :norte, 4326))
            """, nativeQuery = true)
    List<Reporte> buscarAprobadosEnArea(@Param("oeste") double oeste,
                                        @Param("sur") double sur,
                                        @Param("este") double este,
                                        @Param("norte") double norte);

    long countByEstado(EstadoReporte estado);
}
