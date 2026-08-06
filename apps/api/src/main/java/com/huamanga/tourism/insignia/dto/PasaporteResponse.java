package com.huamanga.tourism.insignia.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pasaporte patrimonial (RF-39b).
 *
 * <p>Se devuelven tambien las insignias <strong>no obtenidas</strong>, con su
 * descripcion. Un pasaporte que solo muestra lo conseguido no invita a nada;
 * ver lo que falta y como lograrlo es justo lo que hace que la gamificacion
 * funcione.</p>
 */
@Schema(description = "Sellos, insignias y progreso por ruta de un visitante")
public record PasaporteResponse(

        @Schema(description = "Lugares distintos visitados")
        long sellos,

        @Schema(description = "Total de lugares publicados, para el porcentaje")
        long lugaresTotales,

        List<Sello> visitas,
        List<InsigniaResponse> insignias,
        List<ProgresoRuta> rutas
) {

    @Schema(description = "Un lugar visitado, con la fecha de la primera visita")
    public record Sello(
            UUID lugarId,
            String slug,
            String nombre,
            String categoria,
            String colorCategoria,
            Instant visitadoEn
    ) {
    }

    @Schema(description = "Insignia, obtenida o por obtener")
    public record InsigniaResponse(
            UUID id,
            String codigo,
            String nombre,
            String descripcion,
            String icono,
            boolean obtenida,

            @Schema(description = "Null si aun no se ha obtenido")
            Instant obtenidaEn
    ) {
    }

    /**
     * Progreso en una ruta tematica.
     *
     * <p>{@code visitados} y {@code total} se calculan con COUNT en cada
     * peticion; no existe ningun contador almacenado. Guardarlo seria un
     * atributo derivado y rompería la 3FN, igual que guardar el promedio de
     * calificacion en {@code lugar}.</p>
     */
    @Schema(description = "Avance en una ruta, calculado al vuelo")
    public record ProgresoRuta(
            UUID rutaId,
            String slug,
            String nombre,
            String colorHex,
            long visitados,
            long total,
            boolean completada
    ) {
    }
}
