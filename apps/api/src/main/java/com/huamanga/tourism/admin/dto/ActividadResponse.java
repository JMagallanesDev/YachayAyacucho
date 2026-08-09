package com.huamanga.tourism.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Una linea del registro de actividad (RF-56).
 *
 * <p>Muestra el nombre y el correo de quien actuo, no su identificador: un log
 * que hay que descifrar cruzando UUID a mano no lo lee nadie, y la utilidad de
 * una bitacora es exactamente que se pueda leer de un vistazo.</p>
 */
@Schema(description = "Accion administrativa registrada")
public record ActividadResponse(

        UUID id,

        @Schema(example = "APROBAR_FOTO")
        String accion,

        @Schema(example = "Foto")
        String entidad,

        UUID entidadId,

        @Schema(description = "Contexto de la accion, en JSON")
        String detalles,

        String autorNombre,

        String autorEmail,

        @Schema(description = "Desde donde se ejercio el privilegio (RF-56)")
        String ip,

        Instant ocurridoEn
) {
}
