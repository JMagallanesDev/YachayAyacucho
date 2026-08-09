package com.huamanga.tourism.evento.dto;

import com.huamanga.tourism.evento.domain.EstadoEvento;
import com.huamanga.tourism.evento.domain.TipoEvento;
import com.huamanga.tourism.evento.validacion.FechasCoherentes;
import com.huamanga.tourism.lugar.validacion.TraduccionEspanolObligatoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Alta o edicion de un evento de la agenda (RF-86).
 *
 * <p>Las fechas son {@link LocalDate} y no instantes, y esa eleccion es
 * deliberada: "la fiesta es el 5 de abril" no es un momento en el tiempo, es un
 * dia del calendario. Guardarlo como instante obligaria a elegir una hora
 * arbitraria y lo expondria a desplazarse de dia al cambiar de huso horario,
 * que es justo el fallo que costo el Bloque 4.</p>
 */
@Schema(description = "Datos de un evento cultural")
@TraduccionEspanolObligatoria
@FechasCoherentes
public record EventoRequest(

        @Schema(description = "Lugar patrimonial donde ocurre; opcional, hay fiestas de distrito")
        UUID lugarId,

        @NotNull(message = "{evento.distrito.obligatorio}")
        UUID distritoId,

        @NotNull(message = "{evento.tipo.obligatorio}")
        TipoEvento tipo,

        @Schema(example = "2027-03-21", description = "Dia de calendario, sin hora ni zona")
        @NotNull(message = "{evento.fechaInicio.obligatoria}")
        LocalDate fechaInicio,

        @Schema(description = "Ultimo dia, incluido. Igual a fechaInicio si dura un dia")
        @NotNull(message = "{evento.fechaFin.obligatoria}")
        LocalDate fechaFin,

        @Size(max = 500, message = "{evento.portada.larga}")
        String cloudinaryUrlPortada,

        @Schema(description = "Identificador de YouTube (11 caracteres), NO la URL completa",
                example = "dQw4w9WgXcQ")
        @Pattern(regexp = "^$|^[A-Za-z0-9_-]{11}$", message = "{evento.video.formato}")
        String youtubeVideoId,

        @Schema(description = "Marcar las festividades que se repiten cada anio: habilita el clonado")
        boolean recurrenteAnual,

        @NotNull(message = "{evento.estado.obligatorio}")
        EstadoEvento estado,

        @Valid
        @NotEmpty(message = "{evento.traducciones.vacias}")
        List<EventoTraduccionRequest> traducciones
) {
}
