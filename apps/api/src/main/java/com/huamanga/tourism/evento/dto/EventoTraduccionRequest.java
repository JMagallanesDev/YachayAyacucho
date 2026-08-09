package com.huamanga.tourism.evento.dto;

import com.huamanga.tourism.common.domain.Idioma;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contenido de un evento en un idioma (RF-64, RF-86). */
@Schema(description = "Traduccion de un evento")
public record EventoTraduccionRequest(

        @NotNull(message = "{traduccion.idioma.obligatorio}")
        Idioma idioma,

        @Schema(example = "Semana Santa de Ayacucho")
        @NotBlank(message = "{evento.nombre.obligatorio}")
        @Size(max = 200, message = "{traduccion.nombre.longitud}")
        String nombre,

        String descripcion,

        @Size(max = 200, message = "{evento.organizador.largo}")
        String organizador
) {
}
