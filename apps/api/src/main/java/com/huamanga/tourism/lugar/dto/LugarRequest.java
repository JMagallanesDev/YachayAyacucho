package com.huamanga.tourism.lugar.dto;

import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.validacion.DentroDeAyacucho;
import com.huamanga.tourism.lugar.validacion.HorariosCoherentes;
import com.huamanga.tourism.lugar.validacion.TraduccionEspanolObligatoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Alta o edicion completa de un lugar patrimonial (RF-47).
 *
 * <p>Trae el lugar, sus traducciones y su grilla semanal de horarios en una
 * sola peticion, porque los tres se guardan en una sola transaccion: un lugar
 * publicado sin traduccion en espanol o sin horarios seria un estado invalido
 * del dominio, no un paso intermedio aceptable.</p>
 */
@Schema(description = "Datos completos de un lugar patrimonial")
@DentroDeAyacucho
@TraduccionEspanolObligatoria
@HorariosCoherentes
public record LugarRequest(

        @Schema(example = "catedral-de-ayacucho")
        @NotNull(message = "{lugar.slug.obligatorio}")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "{lugar.slug.formato}")
        @Size(max = 150, message = "{lugar.slug.longitud}")
        String slug,

        @NotNull(message = "{lugar.categoria.obligatoria}")
        UUID categoriaId,

        @NotNull(message = "{lugar.distrito.obligatorio}")
        UUID distritoId,

        @Schema(description = "Longitud en grados decimales (WGS84)", example = "-74.2236")
        @NotNull(message = "{lugar.longitud.obligatoria}")
        Double longitud,

        @Schema(description = "Latitud en grados decimales (WGS84)", example = "-13.1588")
        @NotNull(message = "{lugar.latitud.obligatoria}")
        Double latitud,

        @Size(max = 255, message = "{lugar.direccion.longitud}")
        String direccion,

        @Size(max = 30, message = "{lugar.telefono.longitud}")
        String telefono,

        // ---- Bloque "Antes de ir" (RF-09d). Todo nullable: son datos que se
        // ---- verifican en campo y no siempre se conocen al crear la ficha.
        @DecimalMin(value = "0.0", message = "{lugar.precio.negativo}")
        @Digits(integer = 6, fraction = 2, message = "{lugar.precio.formato}")
        BigDecimal precioEntradaPen,

        @Positive(message = "{lugar.duracion.positiva}")
        Short duracionVisitaMin,

        Boolean aceptaTarjeta,
        Boolean tieneBanos,
        Boolean accesibleSillaRuedas,
        Boolean aptoNinos,

        @DecimalMin(value = "0.0", message = "{lugar.taxi.negativo}")
        @Digits(integer = 6, fraction = 2, message = "{lugar.taxi.formato}")
        BigDecimal costoTaxiDesdePlazaPen,

        Boolean requiereGuia,

        @NotNull(message = "{lugar.estado.obligatorio}")
        EstadoLugar estado,

        // @Valid va sobre el tipo del elemento, no sobre la lista: aplicarlo
        // al contenedor esta deprecado en Bean Validation 3.
        @NotEmpty(message = "{lugar.traducciones.vacias}")
        List<@Valid LugarTraduccionRequest> traducciones,

        List<@Valid HorarioRequest> horarios
) {

    public LugarRequest {
        // Normalizar a lista vacia evita comprobaciones de null repartidas por
        // el validador, el service y el mapper.
        traducciones = traducciones == null ? List.of() : List.copyOf(traducciones);
        horarios = horarios == null ? List.of() : List.copyOf(horarios);
    }
}
