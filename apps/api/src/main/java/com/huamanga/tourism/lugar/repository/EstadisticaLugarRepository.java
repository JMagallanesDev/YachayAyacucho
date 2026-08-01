package com.huamanga.tourism.lugar.repository;

import com.huamanga.tourism.lugar.domain.EstadisticaLugar;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Lectura de la vista materializada.
 *
 * <p>Es la <strong>unica</strong> fuente de la calificacion promedio de un
 * lugar: no existe ni trigger ni columna de promedio en {@code lugar}.</p>
 */
public interface EstadisticaLugarRepository extends JpaRepository<EstadisticaLugar, UUID> {

    /** Ranking "mejor valorados" (RF-06). Usa idx_estadistica_calificacion. */
    @Query("""
            SELECT e FROM EstadisticaLugar e
            WHERE e.totalResenas > 0
            ORDER BY e.calificacionPromedio DESC, e.totalResenas DESC
            """)
    List<EstadisticaLugar> mejorValorados(Pageable pageable);

    /** Ranking "mas visitados" (RF-06). Usa idx_estadistica_visitas. */
    @Query("SELECT e FROM EstadisticaLugar e ORDER BY e.totalVisitas DESC")
    List<EstadisticaLugar> masVisitados(Pageable pageable);
}
