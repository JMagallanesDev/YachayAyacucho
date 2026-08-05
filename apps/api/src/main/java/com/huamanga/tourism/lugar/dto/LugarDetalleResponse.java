package com.huamanga.tourism.lugar.dto;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.lugar.domain.EstadoLugar;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ficha completa de un lugar, ya resuelta a un idioma (RF-09).
 *
 * <p>Nunca expone la entidad JPA: el cliente recibe el texto del idioma que
 * pidio, no el conjunto de traducciones, y no ve nada de la estructura
 * interna ni de las columnas de auditoria.</p>
 */
@Schema(description = "Ficha completa de un lugar patrimonial")
public record LugarDetalleResponse(

        UUID id,
        String slug,

        @Schema(description = "Idioma en el que viene el contenido")
        Idioma idioma,

        @Schema(description = "True si no habia traduccion al idioma pedido y se devolvio la espanola")
        boolean traduccionPorDefecto,

        String nombre,
        String descripcion,
        String historia,
        String consejos,

        CategoriaResponse categoria,
        DistritoResponse distrito,

        @Schema(example = "-74.2236")
        double longitud,

        @Schema(example = "-13.1588")
        double latitud,

        String direccion,
        String telefono,

        // ---- Bloque "Antes de ir" (RF-09d)
        BigDecimal precioEntradaPen,
        Short duracionVisitaMin,
        Boolean aceptaTarjeta,
        Boolean tieneBanos,
        Boolean accesibleSillaRuedas,
        Boolean aptoNinos,
        BigDecimal costoTaxiDesdePlazaPen,
        Boolean requiereGuia,

        EstadoLugar estado,

        @Schema(description = "Grilla semanal ordenada por dia y hora de apertura")
        List<HorarioResponse> horarios,

        /**
         * Estado calculado al momento a partir de la grilla horaria (RF-09b).
         *
         * <p>Se calcula en el servidor y no en el navegador porque depende del
         * huso horario correcto: el reloj del visitante puede estar en
         * cualquier zona, y el lugar abre segun la hora de Ayacucho.</p>
         */
        @Schema(description = "Si el lugar esta abierto en este momento")
        boolean abiertoAhora,

        @Schema(description = "Fotos aprobadas por moderacion, para la galeria (RF-10)")
        List<FotoResponse> fotos,

        Instant actualizadoEn
) {

    @Schema(description = "Categoria del lugar, con su nombre traducido")
    public record CategoriaResponse(UUID id, String codigo, String nombre, String icono, String colorHex) {
    }

    /**
     * Foto lista para pintar.
     *
     * <p>Solo salen la URL publica y el identificador. El {@code public_id} de
     * Cloudinary se queda dentro: es la llave para borrar el binario del CDN y
     * no tiene por que viajar al navegador.</p>
     */
    @Schema(description = "Foto aprobada de un lugar")
    public record FotoResponse(UUID id, String url) {
    }

    @Schema(description = "Distrito y provincia a los que pertenece")
    public record DistritoResponse(UUID id, String codigo, String nombre, String provincia) {
    }
}
