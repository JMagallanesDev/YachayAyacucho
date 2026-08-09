package com.huamanga.tourism.negocio.dto;

import com.huamanga.tourism.negocio.domain.EstadoNegocio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Un negocio del directorio (RF-105).
 *
 * <p><strong>El RUC no esta aqui, y es deliberado.</strong> Es un dato fiscal
 * que se pide para verificar al negocio, no para publicarlo: exponerlo en el
 * listado publico lo pondria al alcance de cualquiera que raspe la pagina. Solo
 * viaja en la respuesta del panel propio y en la del administrador.</p>
 */
@Schema(description = "Negocio local del directorio")
public record NegocioResponse(

        UUID id,

        String nombre,

        String descripcion,

        String categoriaCodigo,

        String categoriaNombre,

        String categoriaIcono,

        String distritoNombre,

        String telefono,

        @Schema(description = "Numero normalizado a formato internacional, sin signos")
        String whatsapp,

        String direccion,

        Double longitud,

        Double latitud,

        String horarioTexto,

        @Schema(description = "Solo se publica APROBADO; el resto solo lo ve su dueno o un admin")
        EstadoNegocio estado,

        Instant registradoEn
) {
}
