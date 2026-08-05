package com.huamanga.tourism.lugar.dto;

import com.huamanga.tourism.lugar.domain.EstadoLugar;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Version reducida para el listado (RF-01).
 *
 * <p>Deliberadamente sin historia ni consejos: un listado paginado no
 * necesita el texto completo de cada ficha, y enviarlo multiplicaria el peso
 * de la respuesta sin que nadie lo lea (RNF-02).</p>
 *
 * <p>Si trae, en cambio, tres cosas que la tarjeta necesita:</p>
 * <ul>
 *   <li>Las <strong>coordenadas</strong>, para que el navegador calcule la
 *       distancia a pie sin una llamada extra por tarjeta (RF-09c).</li>
 *   <li>Los <strong>horarios</strong>, para pintar el badge abierto/cerrado
 *       en el cliente (RF-09b). Se envian en vez del booleano ya calculado
 *       porque estas paginas se sirven cacheadas con ISR: un "abierto" que
 *       se congelo hace horas mentiria, mientras que los horarios no
 *       caducan.</li>
 *   <li>Las <strong>estadisticas</strong> de la vista materializada, para
 *       los rankings y el filtro por calificacion (RF-05, RF-06).</li>
 * </ul>
 */
@Schema(description = "Lugar en su version resumida, para listados")
public record LugarResumenResponse(

        UUID id,
        String slug,
        String nombre,
        String descripcion,
        LugarDetalleResponse.CategoriaResponse categoria,

        @Schema(example = "-74.2236")
        double longitud,

        @Schema(example = "-13.1588")
        double latitud,

        BigDecimal precioEntradaPen,
        Short duracionVisitaMin,
        EstadoLugar estado,

        @Schema(description = "Grilla horaria, para calcular el estado abierto/cerrado en el cliente")
        List<HorarioResponse> horarios,

        @Schema(description = "Media de las resenas publicadas, de la vista materializada")
        BigDecimal calificacionPromedio,

        long totalResenas,
        long totalVisitas
) {
}
