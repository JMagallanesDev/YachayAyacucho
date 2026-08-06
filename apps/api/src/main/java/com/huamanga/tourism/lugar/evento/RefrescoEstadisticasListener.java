package com.huamanga.tourism.lugar.evento;

import com.huamanga.tourism.lugar.service.EstadisticaLugarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Carril rapido de refresco de la vista materializada.
 *
 * <p><strong>El problema que resuelve.</strong> El promedio de un lugar se lee
 * solo de {@code estadistica_lugar}, que se refresca cada 5 minutos. Si alguien
 * puntua y el promedio no se mueve en cinco minutos, la aplicacion parece rota
 * y la reaccion natural es volver a puntuar. La solucion facil —guardar el
 * promedio en una columna de {@code lugar}— rompe la 3FN, asi que no se toca el
 * modelo: se refresca antes.</p>
 *
 * <p><strong>Por que coalescente y no un refresco por reseña.</strong>
 * {@code REFRESH MATERIALIZED VIEW CONCURRENTLY} recalcula la vista entera. Con
 * una reseña por segundo, refrescar en cada una encadenaria refrescos sin
 * descanso. Aqui las peticiones se agrupan: llegue una o cien, se refresca como
 * mucho una vez cada {@code intervalo}.</p>
 *
 * <p>El job de 5 minutos sigue existiendo como suelo de seguridad: cubre lo que
 * cambia sin pasar por la aplicacion, como los seeds cargados por SQL.</p>
 */
@Component
public class RefrescoEstadisticasListener {

    private static final Logger log = LoggerFactory.getLogger(RefrescoEstadisticasListener.class);

    private final EstadisticaLugarService estadisticaLugarService;
    private final Duration intervalo;

    /** Hay cambios sin recalcular. */
    private final AtomicBoolean sucia = new AtomicBoolean(false);
    private final AtomicLong ultimoRefresco = new AtomicLong(0);

    public RefrescoEstadisticasListener(
            EstadisticaLugarService estadisticaLugarService,
            @Value("${app.estadisticas.intervalo-carril-rapido:PT30S}") Duration intervalo) {
        this.estadisticaLugarService = estadisticaLugarService;
        this.intervalo = intervalo;
    }

    /**
     * Marca la vista como desfasada.
     *
     * <p>Se consume DESPUES del commit: antes, el recalculo leeria una reseña
     * aun sin confirmar —o una que luego se revierte— y dejaria el promedio mal
     * hasta el siguiente ciclo. Marcar es todo lo que se hace aqui; refrescar
     * dentro del hilo de la peticion la haria esperar por algo que no le
     * incumbe.</p>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarCalificacion(ContenidoCalificadoEvent evento) {
        sucia.set(true);
        log.debug("Estadisticas marcadas como desfasadas por el lugar {}", evento.slug());
    }

    /**
     * Refresca si hay algo pendiente y ya paso el intervalo.
     *
     * <p>Se ejecuta cada 5 segundos, pero la mayoria de las veces no hace nada:
     * la comprobacion es un booleano en memoria. No lleva {@code @SchedulerLock}
     * porque el refresco es idempotente y {@code CONCURRENTLY} ya deja la vista
     * consultable; que dos instancias coincidan alguna vez cuesta una consulta
     * de mas, mientras que un lock distribuido cada cinco segundos costaria una
     * ida y vuelta a Redis permanente.</p>
     */
    @Scheduled(fixedDelay = 5_000)
    public void refrescarSiHaceFalta() {
        if (!sucia.get()) {
            return;
        }

        long ahora = System.currentTimeMillis();
        if (ahora - ultimoRefresco.get() < intervalo.toMillis()) {
            return;
        }

        // Se limpia la marca ANTES de refrescar. Al reves habria una ventana en
        // la que una reseña nueva marcaria la vista como sucia y el refresco en
        // curso —que no la incluye— borraria esa marca al terminar, dejando el
        // cambio sin recalcular hasta el job de 5 minutos.
        sucia.set(false);
        ultimoRefresco.set(ahora);

        try {
            estadisticaLugarService.refrescar();
        } catch (Exception e) {
            // Se vuelve a marcar para reintentarlo en el siguiente ciclo.
            sucia.set(true);
            log.warn("No se pudo refrescar la vista en el carril rapido: {}", e.getMessage());
        }
    }
}
