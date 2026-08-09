package com.huamanga.tourism.lugar.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Par de fotos para el slider antes/despues (RF-11, RF-11b).
 *
 * <p>{@code urlActual} puede faltar, y el frontend tiene que contar con ello:
 * hay fotos historicas de las que todavia no se ha tomado la contraparte
 * moderna. En ese caso <strong>no se muestra el slider</strong> —una
 * comparacion con un solo lado no es una comparacion— sino la foto historica
 * sola con su año y su credito.</p>
 *
 * <p>{@code puntoCaptura} es el sitio desde el que se tomo la foto antigua, y
 * es lo que hace posible el modo «Parate aqui»: si estas a menos de 50 m de
 * ese punto, la aplicacion te ofrece ver la comparacion justo donde se planto
 * el fotografo.</p>
 */
@Schema(description = "Foto historica y su contraparte actual")
public record ImagenHistoricaResponse(

        UUID id,

        String titulo,

        String urlHistorica,

        @Schema(description = "Año de la foto antigua", example = "1920")
        short anioHistorico,

        @Schema(description = "Foto actual; si falta, no hay comparacion que mostrar")
        String urlActual,

        @Schema(description = "Autoria o archivo de procedencia de la foto antigua")
        String creditoHistorico,

        @Schema(description = "Longitud del punto de captura; null si no se conoce")
        Double longitudCaptura,

        Double latitudCaptura,

        short orden
) {

    /** Sin foto actual no hay dos lados que comparar. */
    public boolean tieneComparacion() {
        return urlActual != null && !urlActual.isBlank();
    }
}
