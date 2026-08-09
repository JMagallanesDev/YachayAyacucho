package com.huamanga.tourism.ruta.dto;

import com.huamanga.tourism.common.domain.Idioma;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Alta o edicion de una ruta tematica (RF-53).
 *
 * <p>Las paradas llegan como una <strong>lista ordenada de identificadores de
 * lugar</strong>, no como objetos con un campo {@code orden}. Es una decision
 * pequena con consecuencias: el orden lo da la posicion en la lista, asi que es
 * imposible enviar dos paradas con el mismo numero o saltarse el 3. El servidor
 * numera de 1 a N al guardar.</p>
 */
@Schema(description = "Datos de una ruta tematica")
public record RutaRequest(

        @Schema(example = "ruta-colonial")
        @NotBlank(message = "{ruta.slug.obligatorio}")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "{ruta.slug.formato}")
        @Size(max = 150, message = "{ruta.slug.longitud}")
        String slug,

        @Schema(example = "#B3202B")
        @NotBlank(message = "{ruta.color.obligatorio}")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{ruta.color.formato}")
        String colorHex,

        @Size(max = 50, message = "{ruta.icono.longitud}")
        String icono,

        boolean activa,

        @Schema(description = "Posicion de la ruta en el listado")
        Short orden,

        @Valid
        @NotEmpty(message = "{ruta.traducciones.vacias}")
        List<RutaTraduccionRequest> traducciones,

        @Schema(description = "Lugares EN ORDEN DE RECORRIDO; la posicion define el orden")
        @NotEmpty(message = "{ruta.paradas.vacias}")
        @Size(min = 2, message = "{ruta.paradas.minimo}")
        List<UUID> paradas
) {

    @Schema(description = "Nombre y descripcion de la ruta en un idioma")
    public record RutaTraduccionRequest(

            @NotNull(message = "{traduccion.idioma.obligatorio}")
            Idioma idioma,

            @NotBlank(message = "{ruta.nombre.obligatorio}")
            @Size(max = 200, message = "{traduccion.nombre.longitud}")
            String nombre,

            String descripcion
    ) {
    }
}
