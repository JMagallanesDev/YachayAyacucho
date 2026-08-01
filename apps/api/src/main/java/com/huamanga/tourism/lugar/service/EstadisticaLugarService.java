package com.huamanga.tourism.lugar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Refresca la vista materializada de estadisticas por lugar.
 */
@Service
public class EstadisticaLugarService {

    private static final Logger log = LoggerFactory.getLogger(EstadisticaLugarService.class);

    private static final String SQL_REFRESCO =
            "REFRESH MATERIALIZED VIEW CONCURRENTLY estadistica_lugar";

    private final JdbcTemplate jdbcTemplate;

    public EstadisticaLugarService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Recalcula los agregados de resenas, check-ins y favoritos.
     *
     * <p>CONCURRENTLY es lo que permite que el ranking siga consultable
     * mientras se refresca: sin el, PostgreSQL bloquea la vista entera. A
     * cambio exige un indice unico sobre {@code lugar_id}, que crea la
     * migracion V13.</p>
     *
     * <p>Este metodo <strong>no</strong> lleva {@code @Transactional} a
     * proposito: PostgreSQL prohibe ejecutar REFRESH ... CONCURRENTLY dentro
     * de un bloque de transaccion.</p>
     */
    public void refrescar() {
        long inicio = System.nanoTime();
        jdbcTemplate.execute(SQL_REFRESCO);
        long milis = (System.nanoTime() - inicio) / 1_000_000;
        log.info("Vista materializada estadistica_lugar refrescada en {} ms", milis);
    }
}
