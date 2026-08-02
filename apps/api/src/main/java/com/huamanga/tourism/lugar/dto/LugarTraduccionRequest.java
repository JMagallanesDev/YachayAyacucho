package com.huamanga.tourism.lugar.dto;

import com.huamanga.tourism.common.domain.Idioma;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contenido editorial de un lugar en un idioma.
 *
 * <p>Es el texto patrimonial que publica el administrador. Vive en la base de
 * datos y no en los archivos de traduccion del frontend porque es contenido de
 * dominio: auditable, editable sin desplegar y traducible por registro
 * (seccion 5.5 del plan).</p>
 */
@Schema(description = "Contenido de un lugar en un idioma")
public record LugarTraduccionRequest(

        @NotNull(message = "{traduccion.idioma.obligatorio}")
        Idioma idioma,

        @NotBlank(message = "{traduccion.nombre.obligatorio}")
        @Size(max = 200, message = "{traduccion.nombre.longitud}")
        String nombre,

        String descripcion,

        String historia,

        String consejos
) {
}
