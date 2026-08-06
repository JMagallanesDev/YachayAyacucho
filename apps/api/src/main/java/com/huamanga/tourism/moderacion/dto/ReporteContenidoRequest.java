package com.huamanga.tourism.moderacion.dto;

import com.huamanga.tourism.moderacion.domain.MotivoReporteContenido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Reporte de una foto o una reseña inapropiada (RF-45).
 *
 * <p>Refleja en el DTO el mismo XOR que impone la base con
 * {@code ck_reporte_contenido_xor}: exactamente uno de los dos
 * identificadores. La comprobacion se repite aqui para responder un 400 con el
 * campo señalado en vez de un error de integridad de PostgreSQL, que es
 * correcto pero ilegible para quien llama al API.</p>
 */
@Schema(description = "Denuncia de contenido inapropiado")
public record ReporteContenidoRequest(

        @Schema(description = "Foto reportada; excluyente con resenaId")
        UUID fotoId,

        @Schema(description = "Resena reportada; excluyente con fotoId")
        UUID resenaId,

        @NotNull(message = "{reporte.motivo.obligatorio}")
        MotivoReporteContenido motivo
) {

    /**
     * Exactamente uno de los dos, nunca ambos ni ninguno.
     *
     * <p>Se nombra {@code contenido} para que el error de validacion salga
     * bajo esa clave y el frontend pueda mostrarlo junto al selector, en vez de
     * como un error global sin sitio donde pintarse.</p>
     */
    @AssertTrue(message = "{reporte.contenido.exclusivo}")
    public boolean isContenidoValido() {
        return (fotoId != null) ^ (resenaId != null);
    }
}
