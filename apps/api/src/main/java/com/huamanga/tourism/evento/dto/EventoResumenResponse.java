package com.huamanga.tourism.evento.dto;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.evento.domain.EstadoEvento;
import com.huamanga.tourism.evento.domain.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento en el calendario, en la portada o en un listado (RF-79, RF-84).
 *
 * <p>Las fechas viajan como {@code LocalDate}, que Jackson serializa a
 * {@code "2027-03-21"}: un dia de calendario sin hora ni zona. El frontend
 * tiene prohibido convertirlas a {@code Date} sin fijar la zona, porque en
 * cualquier huso al oeste de Greenwich eso resta un dia.</p>
 */
@Schema(description = "Evento para calendario y listados")
public record EventoResumenResponse(

        UUID id,

        Idioma idioma,

        @Schema(description = "true si se cayo al espanol por no haber traduccion")
        boolean traduccionSustituta,

        String nombre,

        String descripcion,

        String organizador,

        TipoEvento tipo,

        @Schema(example = "2027-03-21")
        LocalDate fechaInicio,

        @Schema(example = "2027-03-28", description = "Ultimo dia, incluido")
        LocalDate fechaFin,

        @Schema(description = "Dias que dura, contando el primero y el ultimo")
        long duracionDias,

        String cloudinaryUrlPortada,

        @Schema(description = "Nombre del lugar patrimonial, si el evento ocurre en uno")
        String lugarNombre,

        @Schema(description = "Slug del lugar, para enlazar a su ficha")
        String lugarSlug,

        String distritoNombre,

        boolean recurrenteAnual,

        EstadoEvento estado
) {
}
