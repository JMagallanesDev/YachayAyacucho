package com.huamanga.tourism.lugar.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara el refresco de {@code estadistica_lugar} cada 5 minutos
 * (seccion 6.3 del plan).
 *
 * <p>Se separa del service para que la logica de refresco sea invocable
 * directamente en los tests, sin depender del planificador ni de Redis.</p>
 */
@Component
public class EstadisticaLugarJob {

    private static final Logger log = LoggerFactory.getLogger(EstadisticaLugarJob.class);

    private final EstadisticaLugarService estadisticaLugarService;

    public EstadisticaLugarJob(EstadisticaLugarService estadisticaLugarService) {
        this.estadisticaLugarService = estadisticaLugarService;
    }

    /**
     * {@code lockAtMostFor} evita que una instancia caida deje el lock
     * retenido para siempre; {@code lockAtLeastFor} impide que dos instancias
     * con relojes ligeramente desfasados lo ejecuten dos veces seguidas.
     */
    @Scheduled(cron = "${app.estadisticas.refresco-cron}")
    @SchedulerLock(
            name = "refrescarEstadisticaLugar",
            lockAtMostFor = "${app.estadisticas.lock-maximo}",
            lockAtLeastFor = "${app.estadisticas.lock-minimo}")
    public void refrescarEstadisticas() {
        try {
            estadisticaLugarService.refrescar();
        } catch (Exception ex) {
            // Un fallo aqui no puede tumbar el planificador: los rankings se
            // quedan con los datos del ciclo anterior y se reintenta en 5 min.
            log.error("Fallo el refresco de la vista materializada estadistica_lugar", ex);
        }
    }
}
