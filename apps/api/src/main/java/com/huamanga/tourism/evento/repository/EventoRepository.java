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
import java.util.Optional;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    Page<Evento> findByEstadoAndTipo(EstadoEvento estado, TipoEvento tipo, Pageable pageable);

    /**
     * Eventos que se solapan con un rango de fechas: alimenta el calendario
     * mensual (RF-79) y la vista "Durante mi visita" (RF-84b). Usa
     * idx_evento_fecha.
     *
     * <p>La condicion es de <strong>solape</strong>, no de contencion: entra
     * todo evento cuyo tramo toque el rango aunque empiece antes o acabe
     * despues. Es lo que hace que una fiesta de varios dias aparezca en el
     * calendario todos los dias que dura, y que un turista que llega el
     * miercoles vea la festividad que arranco el lunes.</p>
     */
    @Query("""
            SELECT e FROM Evento e
            WHERE e.estado = :estado
              AND e.fechaInicio <= :hasta
              AND e.fechaFin >= :desde
            ORDER BY e.fechaInicio
            """)
    List<Evento> findEnRango(EstadoEvento estado, LocalDate desde, LocalDate hasta);

    /** Igual que el anterior, filtrando ademas por tipo (RF-85). */
    @Query("""
            SELECT e FROM Evento e
            WHERE e.estado = :estado
              AND e.tipo = :tipo
              AND e.fechaInicio <= :hasta
              AND e.fechaFin >= :desde
            ORDER BY e.fechaInicio
            """)
    List<Evento> findEnRangoPorTipo(EstadoEvento estado, TipoEvento tipo,
                                    LocalDate desde, LocalDate hasta);

    /**
     * Proximos eventos con cuenta regresiva en la portada (RF-84).
     *
     * <p>El corte es por {@code fechaFin} y no por {@code fechaInicio}: una
     * fiesta que empezo ayer y termina manana sigue siendo un proximo evento
     * para quien esta en la ciudad hoy. Filtrar por la fecha de inicio la
     * escondria justo cuando esta ocurriendo.</p>
     */
    @Query("""
            SELECT e FROM Evento e
            WHERE e.estado = :estado
              AND e.fechaFin >= :desde
            ORDER BY e.fechaInicio
            """)
    List<Evento> findProximos(EstadoEvento estado, LocalDate desde, Pageable pagina);

    @Query("""
            SELECT e FROM Evento e
            WHERE e.estado = :estado
              AND e.tipo = :tipo
              AND e.fechaFin >= :desde
            ORDER BY e.fechaInicio
            """)
    List<Evento> findProximosPorTipo(EstadoEvento estado, TipoEvento tipo,
                                     LocalDate desde, Pageable pagina);

    List<Evento> findByRecurrenteAnualTrue();

    /** Bandeja del administrador: incluye borradores y cancelados. */
    List<Evento> findAllByOrderByFechaInicioDesc();

    /**
     * Ficha completa sin caer en N+1: las traducciones se traen en la misma
     * consulta. El lugar y el distrito se dejan perezosos porque el mapper solo
     * lee su nombre y son a-uno, no colecciones.
     */
    @Query("""
            SELECT e FROM Evento e
            LEFT JOIN FETCH e.traducciones
            WHERE e.id = :id
            """)
    Optional<Evento> findByIdConTraducciones(UUID id);

    /**
     * Comprueba si ya existe un clon de este evento en un anio concreto.
     *
     * <p>Compara contra el anio de inicio del clon, no contra un campo "anio"
     * que no existe: la fecha es el unico dato fiable.</p>
     */
    @Query("""
            SELECT COUNT(e) > 0 FROM Evento e
            WHERE e.eventoOrigen.id = :origenId
              AND YEAR(e.fechaInicio) = :anio
            """)
    boolean existeClonDe(UUID origenId, int anio);
}
