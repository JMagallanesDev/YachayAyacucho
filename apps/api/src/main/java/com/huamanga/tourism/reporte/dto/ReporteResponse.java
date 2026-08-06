package com.huamanga.tourism.reporte.dto;

import com.huamanga.tourism.reporte.domain.EstadoReporte;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reporte tal como lo ve el publico y su propio autor.
 *
 * <p><strong>Nunca lleva quien lo envio</strong>, ni siquiera en los reportes
 * no anonimos. El nombre del denunciante solo tiene sentido para la moderacion;
 * publicarlo en un mapa de denuncias expondria a esa persona frente a quien
 * denuncio, que es exactamente lo que este modulo trata de evitar.</p>
 *
 * @param notasAdmin solo se rellena en la bandeja de moderacion; en las
 *                   respuestas publicas viaja siempre a null
 */
@Schema(description = "Incidente reportado")
public record ReporteResponse(

        UUID id,
        String tipoCodigo,
        String tipoNombre,
        String tipoIcono,
        String colorHex,

        String descripcion,
        double longitud,
        double latitud,
        String direccionReferencial,

        EstadoReporte estado,
        boolean esAnonimo,

        List<String> fotos,

        @Schema(description = "Notas internas; null fuera de la moderacion")
        String notasAdmin,

        Instant reportadoEn
) {
}
