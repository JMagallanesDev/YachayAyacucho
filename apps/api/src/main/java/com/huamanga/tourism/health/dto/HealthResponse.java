package com.huamanga.tourism.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta del endpoint {@code GET /api/v1/health}.
 *
 * <p>El estado global es UP unicamente si todos los componentes estan UP:
 * un sistema que no puede hablar con su base de datos no esta sano aunque
 * el proceso siga vivo.</p>
 */
@Schema(description = "Estado operativo del sistema y de sus componentes")
public record HealthResponse(

        @Schema(description = "Estado global: UP solo si todos los componentes estan UP", example = "UP")
        HealthStatus status,

        @Schema(description = "Nombre de la aplicacion", example = "yachay-api")
        String application,

        @Schema(description = "Momento de la comprobacion (UTC, ISO-8601)")
        Instant timestamp,

        @Schema(description = "Detalle por componente")
        List<ComponentStatus> components
) {

    /**
     * Construye la respuesta derivando el estado global de los componentes.
     */
    public static HealthResponse from(String application, Instant timestamp, List<ComponentStatus> components) {
        HealthStatus global = components.stream().allMatch(c -> c.status() == HealthStatus.UP)
                ? HealthStatus.UP
                : HealthStatus.DOWN;
        return new HealthResponse(global, application, timestamp, List.copyOf(components));
    }
}
