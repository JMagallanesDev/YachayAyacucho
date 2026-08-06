package com.huamanga.tourism.reporte.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Denuncia ciudadana de un atentado al patrimonio (RF-69 a RF-72).
 *
 * <p>Solo tres campos son obligatorios —tipo, descripcion y ubicacion— porque
 * el requisito pide que el formulario se complete en menos de 60 segundos. Todo
 * lo demas es opcional, incluido identificarse.</p>
 *
 * @param esAnonimo por defecto <strong>true</strong>. Es deliberado: quien
 *                  quiera dar su nombre lo activa, no al reves. Un formulario
 *                  que identifica por omision hace que la gente descubra
 *                  tarde que quedo señalada
 */
@Schema(description = "Reporte ciudadano de un incidente")
public record ReporteRequest(

        @NotNull(message = "{reporte.tipo.obligatorio}")
        UUID tipoIncidenteId,

        @Schema(example = "Han pintado con aerosol el muro lateral del templo.")
        @NotBlank(message = "{reporte.descripcion.obligatoria}")
        @Size(max = 2000, message = "{reporte.descripcion.larga}")
        String descripcion,

        @Schema(example = "-74.2236")
        @NotNull(message = "{reporte.longitud.obligatoria}")
        @DecimalMin(value = "-180.0", message = "{reporte.coordenada.rango}")
        @DecimalMax(value = "180.0", message = "{reporte.coordenada.rango}")
        Double longitud,

        @Schema(example = "-13.1588")
        @NotNull(message = "{reporte.latitud.obligatoria}")
        @DecimalMin(value = "-90.0", message = "{reporte.coordenada.rango}")
        @DecimalMax(value = "90.0", message = "{reporte.coordenada.rango}")
        Double latitud,

        @Schema(description = "Referencia en palabras, por si el pin no basta")
        @Size(max = 255, message = "{reporte.direccion.larga}")
        String direccionReferencial,

        @Schema(description = "Si es false, el reporte queda asociado a la cuenta")
        Boolean esAnonimo,

        @Schema(description = "Solo se usa en reportes NO anonimos")
        @Size(max = 120, message = "{reporte.nombre.largo}")
        String nombreReportante
) {

    /** Ante la duda, anonimo: el valor por defecto protege, no expone. */
    public boolean anonimo() {
        return esAnonimo == null || esAnonimo;
    }
}
