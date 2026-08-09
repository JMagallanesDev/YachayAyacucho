package com.huamanga.tourism.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * Metricas del panel (RF-52).
 *
 * <p>Todo en una sola respuesta a proposito: el dashboard pinta cuatro graficos
 * y ocho totales, y pedirlos con doce llamadas dejaria la pantalla apareciendo a
 * trozos. Son consultas de agregacion sobre tablas pequenas.</p>
 */
@Schema(description = "Totales y series temporales del panel de administracion")
public record DashboardResponse(

        Totales totales,

        @Schema(description = "Visitas por dia de los ultimos 30 dias, sin huecos")
        List<PuntoDiario> visitas,

        @Schema(description = "Registros de usuarios por dia, sin huecos")
        List<PuntoDiario> registros,

        @Schema(description = "Cuantos lugares publicados hay por categoria")
        List<Reparto> lugaresPorCategoria,

        @Schema(description = "Visitas acumuladas por seccion del sitio")
        List<Reparto> visitasPorSeccion,

        @Schema(description = "Lo que espera en las tres bandejas de moderacion")
        Pendientes pendientes
) {

    @Schema(description = "Contadores globales")
    public record Totales(
            long usuarios,
            long lugares,
            long eventos,
            long resenas,
            long fotos,
            long reportes,
            long checkIns,
            long visitasTotales
    ) {
    }

    /** Un dia y su valor. La fecha es un dia de calendario, sin hora ni zona. */
    @Schema(description = "Valor de un dia")
    public record PuntoDiario(LocalDate fecha, long valor) {
    }

    /** Una categoria y su cuenta, para los graficos de reparto. */
    @Schema(description = "Etiqueta y valor")
    public record Reparto(String etiqueta, String color, long valor) {
    }

    @Schema(description = "Cola de moderacion por tipo de contenido")
    public record Pendientes(long fotos, long resenas, long reportes) {

        public long total() {
            return fotos + resenas + reportes;
        }
    }
}
