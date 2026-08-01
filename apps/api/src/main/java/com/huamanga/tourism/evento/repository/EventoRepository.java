package com.huamanga.tourism.evento.repository;

import com.huamanga.tourism.evento.domain.EstadoEvento;
import com.huamanga.tourism.evento.domain.Evento;
import com.huamanga.tourism.evento.domain.TipoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    Page<Evento> findByEstadoAndTipo(EstadoEvento estado, TipoEvento tipo, Pageable pageable);

    /**
     * Eventos que se solapan con un rango de fechas: alimenta el calendario
     * mensual (RF-79) y la vista "Durante mi visita" (RF-84b). Usa
     * idx_evento_fecha.
     */
    @Query("""
            SELECT e FROM Evento e
            WHERE e.estado = :estado
              AND e.fechaInicio <= :hasta
              AND e.fechaFin >= :desde
            ORDER BY e.fechaInicio
            """)
    List<Evento> findEnRango(EstadoEvento estado, LocalDate desde, LocalDate hasta);

    /** Proximos eventos con countdown en la portada (RF-84). */
    List<Evento> findByEstadoAndFechaInicioGreaterThanEqualOrderByFechaInicioAsc(
            EstadoEvento estado, LocalDate desde, Pageable pageable);

    List<Evento> findByRecurrenteAnualTrue();
}
