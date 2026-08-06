package com.huamanga.tourism.reporte.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Uno de los 7 tipos de incidente predefinidos (RF-70). */
@Schema(description = "Tipo de incidente reportable")
public record TipoIncidenteResponse(
        UUID id,
        String codigo,
        String nombre,
        String icono,
        String colorHex
) {
}
