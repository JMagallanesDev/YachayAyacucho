package com.huamanga.tourism.negocio.dto;

import com.huamanga.tourism.common.domain.Idioma;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Alta o edicion de un negocio del directorio (RF-104, RF-107).
 *
 * <p>No lleva {@code estado}: el estado no lo elige quien registra. Un negocio
 * nace PENDIENTE y solo un administrador lo mueve. Si este record admitiera ese
 * campo, bastaria con enviarlo para publicarse sin revision.</p>
 */
@Schema(description = "Datos de un negocio local")
public record NegocioRequest(

        @Schema(example = "Restaurante La Casona")
        @NotBlank(message = "{negocio.nombre.obligatorio}")
        @Size(max = 200, message = "{negocio.nombre.longitud}")
        String nombre,

        @NotNull(message = "{negocio.categoria.obligatoria}")
        UUID categoriaId,

        @NotNull(message = "{negocio.distrito.obligatorio}")
        UUID distritoId,

        @Schema(description = "11 digitos, opcional", example = "20123456789")
        @Pattern(regexp = "^$|^[0-9]{11}$", message = "{negocio.ruc.formato}")
        String ruc,

        @Size(max = 30, message = "{negocio.telefono.longitud}")
        String telefono,

        @Schema(description = "Numero de WhatsApp para el boton de contacto (RF-110)")
        @Size(max = 30, message = "{negocio.whatsapp.longitud}")
        String whatsapp,

        @Size(max = 255, message = "{negocio.direccion.longitud}")
        String direccion,

        @Schema(description = "Longitud; opcional, pero si va una va la otra")
        @DecimalMin(value = "-75.5", message = "{negocio.coordenadas.fuera}")
        @DecimalMax(value = "-73.0", message = "{negocio.coordenadas.fuera}")
        Double longitud,

        @DecimalMin(value = "-15.5", message = "{negocio.coordenadas.fuera}")
        @DecimalMax(value = "-12.5", message = "{negocio.coordenadas.fuera}")
        Double latitud,

        @Schema(description = "Texto libre: el sistema no calcula nada con el")
        @Size(max = 255, message = "{negocio.horario.longitud}")
        String horarioTexto,

        @Valid
        List<DescripcionRequest> traducciones
) {

    @Schema(description = "Descripcion del negocio en un idioma")
    public record DescripcionRequest(

            @NotNull(message = "{traduccion.idioma.obligatorio}")
            Idioma idioma,

            @Size(max = 2000, message = "{negocio.descripcion.longitud}")
            String descripcion
    ) {
    }
}
