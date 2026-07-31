package com.huamanga.tourism.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado de un componente concreto de la infraestructura.
 *
 * <p>El campo {@code detail} es deliberadamente generico: nunca expone
 * el mensaje real de la excepcion, porque puede contener host, puerto o
 * usuario de la base de datos. La causa completa se registra en el log
 * del servidor.</p>
 */
@Schema(description = "Estado de un componente de infraestructura")
public record ComponentStatus(

        @Schema(description = "Nombre del componente", example = "postgresql")
        String name,

        @Schema(description = "Estado del componente", example = "UP")
        HealthStatus status,

        @Schema(description = "Tiempo de respuesta de la comprobacion, en milisegundos", example = "12")
        long responseTimeMs,

        @Schema(description = "Detalle legible del resultado", example = "Conexion establecida")
        String detail
) {

    public static ComponentStatus up(String name, long responseTimeMs, String detail) {
        return new ComponentStatus(name, HealthStatus.UP, responseTimeMs, detail);
    }

    public static ComponentStatus down(String name, long responseTimeMs, String detail) {
        return new ComponentStatus(name, HealthStatus.DOWN, responseTimeMs, detail);
    }
}
