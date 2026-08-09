package com.huamanga.tourism.admin.service;

import com.huamanga.tourism.admin.dto.DashboardResponse;
import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.tiempo.TiempoAyacucho;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Metricas del panel (RF-52).
 *
 * <p>Se resuelve con SQL de agregacion y no con repositorios JPA: son ocho
 * {@code COUNT} y cuatro {@code GROUP BY} sobre tablas distintas, y traerlas
 * como entidades para contarlas en memoria seria cargar filas enteras para
 * tirarlas.</p>
 *
 * <p>Las series diarias se rellenan con {@code generate_series} de PostgreSQL,
 * de modo que <strong>los dias sin actividad llegan como cero y no como
 * huecos</strong>. Sin eso, un grafico de lineas uniria el dia 3 con el dia 9 en
 * una recta y sugeriria una actividad que no existio.</p>
 */
@Service
public class DashboardService {

    /** Ventana de las series temporales del panel. */
    private static final int DIAS_DE_HISTORIA = 30;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public DashboardService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse metricas(Idioma idioma) {
        LocalDate hoy = TiempoAyacucho.hoy(clock);
        LocalDate desde = hoy.minusDays(DIAS_DE_HISTORIA - 1L);

        return new DashboardResponse(
                totales(),
                serieDiaria("""
                        SELECT d.dia::date AS fecha, COALESCE(SUM(v.total_visitas), 0) AS valor
                        FROM generate_series(?::date, ?::date, '1 day') AS d(dia)
                        LEFT JOIN visita_resumen_diario v ON v.fecha = d.dia::date
                        GROUP BY d.dia ORDER BY d.dia
                        """, desde, hoy),
                serieDiaria("""
                        SELECT d.dia::date AS fecha, COUNT(u.id) AS valor
                        FROM generate_series(?::date, ?::date, '1 day') AS d(dia)
                        LEFT JOIN usuario u
                               ON u.created_at >= d.dia
                              AND u.created_at < d.dia + INTERVAL '1 day'
                              AND u.deleted_at IS NULL
                        GROUP BY d.dia ORDER BY d.dia
                        """, desde, hoy),
                lugaresPorCategoria(idioma),
                visitasPorSeccion(),
                pendientes());
    }

    private DashboardResponse.Totales totales() {
        return new DashboardResponse.Totales(
                contar("SELECT COUNT(*) FROM usuario WHERE deleted_at IS NULL"),
                contar("SELECT COUNT(*) FROM lugar WHERE deleted_at IS NULL"),
                contar("SELECT COUNT(*) FROM evento WHERE deleted_at IS NULL"),
                // La tabla `resena` no lleva soft delete por columna: su baja
                // logica es el estado ELIMINADA (V5). Contar por `deleted_at`
                // aqui reventaba la consulta entera.
                contar("SELECT COUNT(*) FROM resena WHERE estado <> 'ELIMINADA'"),
                contar("SELECT COUNT(*) FROM foto"),
                contar("SELECT COUNT(*) FROM reporte WHERE deleted_at IS NULL"),
                contar("SELECT COUNT(*) FROM check_in"),
                contar("SELECT COALESCE(SUM(total_visitas), 0) FROM visita_resumen_diario"));
    }

    /**
     * Reparto de lugares publicados por categoria, con el color del catalogo.
     *
     * <p>El color sale de la propia categoria y no de una paleta del frontend:
     * asi el grafico usa el mismo color que las chinchetas del mapa y los chips
     * del listado, y anadir una categoria manana no obliga a tocar el panel.</p>
     */
    private List<DashboardResponse.Reparto> lugaresPorCategoria(Idioma idioma) {
        return jdbc.query("""
                SELECT COALESCE(t.nombre, c.codigo) AS etiqueta,
                       c.color_hex                  AS color,
                       COUNT(l.id)                  AS valor
                FROM categoria_lugar c
                LEFT JOIN categoria_lugar_traduccion t
                       ON t.categoria_lugar_id = c.id AND t.idioma = ?
                LEFT JOIN lugar l
                       ON l.categoria_lugar_id = c.id
                      AND l.deleted_at IS NULL
                      AND l.estado = 'PUBLICADO'
                GROUP BY c.id, t.nombre, c.codigo, c.color_hex
                HAVING COUNT(l.id) > 0
                ORDER BY valor DESC
                """,
                (fila, i) -> new DashboardResponse.Reparto(
                        fila.getString("etiqueta"), fila.getString("color"), fila.getLong("valor")),
                idioma.name().toLowerCase());
    }

    private List<DashboardResponse.Reparto> visitasPorSeccion() {
        return jdbc.query("""
                SELECT tipo_pagina AS etiqueta, SUM(total_visitas) AS valor
                FROM visita_resumen_diario
                GROUP BY tipo_pagina
                ORDER BY valor DESC
                """,
                // Sin color: el frontend asigna la paleta de graficos, porque
                // estas secciones no son un catalogo con color propio.
                (fila, i) -> new DashboardResponse.Reparto(
                        fila.getString("etiqueta"), null, fila.getLong("valor")));
    }

    /**
     * Lo que espera en las tres bandejas.
     *
     * <p>Las fotos cuentan {@code PENDIENTE} <strong>y</strong> {@code EN_REVISION}:
     * una foto llega al segundo estado por acumular tres denuncias, y hasta el
     * Bloque 10 desaparecia de la galeria sin entrar en ninguna cola. Ese era el
     * cabo suelto anotado en el Bloque 7.</p>
     */
    private DashboardResponse.Pendientes pendientes() {
        return new DashboardResponse.Pendientes(
                contar("SELECT COUNT(*) FROM foto WHERE estado IN ('PENDIENTE', 'EN_REVISION')"),
                contar("SELECT COUNT(*) FROM resena WHERE estado = 'EN_REVISION'"),
                contar("SELECT COUNT(*) FROM reporte WHERE estado IN ('RECIBIDO', 'EN_REVISION') AND deleted_at IS NULL"),
                // Bloque 11: los negocios a la espera de revision entran en el
                // mismo resumen, para que el panel siga necesitando una llamada.
                contar("SELECT COUNT(*) FROM negocio WHERE estado = 'PENDIENTE' AND deleted_at IS NULL"));
    }

    private List<DashboardResponse.PuntoDiario> serieDiaria(String sql, LocalDate desde, LocalDate hasta) {
        return jdbc.query(sql,
                (fila, i) -> new DashboardResponse.PuntoDiario(
                        fila.getObject("fecha", LocalDate.class), fila.getLong("valor")),
                desde, hasta);
    }

    private long contar(String sql) {
        Long total = jdbc.queryForObject(sql, Long.class);
        return total == null ? 0 : total;
    }

    /** Traduce una lista de repartos a un mapa, para los tests. */
    public static Map<String, Long> comoMapa(List<DashboardResponse.Reparto> repartos) {
        return repartos.stream().collect(
                Collectors.toMap(DashboardResponse.Reparto::etiqueta, DashboardResponse.Reparto::valor));
    }
}
