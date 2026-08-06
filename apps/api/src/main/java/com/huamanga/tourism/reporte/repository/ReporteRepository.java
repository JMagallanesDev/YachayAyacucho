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

    /**
     * Lo que llega al mapa publico (RF-74).
     *
     * <p>Amplia la consulta anterior para incluir tambien los {@code RESUELTO}:
     * un incidente que ya se arreglo sigue siendo informacion util —muestra que
     * el mecanismo funciona— y ocultarlo daria la impresion de que nada se
     * resuelve nunca. Lo que jamas aparece es un {@code RECIBIDO} sin revisar.</p>
     */
    @Query(value = """
            SELECT * FROM reporte r
            WHERE r.deleted_at IS NULL
              AND r.estado IN ('APROBADO', 'RESUELTO')
              AND ST_Within(r.ubicacion,
                            ST_MakeEnvelope(:oeste, :sur, :este, :norte, 4326))
            ORDER BY r.created_at DESC
            """, nativeQuery = true)
    List<Reporte> buscarPublicadosEnArea(@Param("oeste") double oeste,
                                         @Param("sur") double sur,
                                         @Param("este") double este,
                                         @Param("norte") double norte);

    /**
     * Reportes de una persona.
     *
     * <p>Solo devuelve los NO anonimos, porque son los unicos que tienen
     * {@code usuario_id}. Un reporte anonimo no aparece ni en «mis reportes»:
     * seria contradictorio que el sistema pudiera decir «esto lo enviaste tu».</p>
     */
    List<Reporte> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);

    /** Bandeja completa de moderacion, en cualquier estado (RF-76). */
    @Query("""
            SELECT r FROM Reporte r
            JOIN FETCH r.tipoIncidente t
            LEFT JOIN FETCH t.traducciones
            ORDER BY r.createdAt DESC
            """)
    List<Reporte> paraModerar(Pageable pagina);

    long countByEstado(EstadoReporte estado);

    /** Insignia GUARDIAN: reportes aprobados y NO anonimos de una persona. */
    long countByUsuarioIdAndEstadoIn(UUID usuarioId, List<EstadoReporte> estados);
}
