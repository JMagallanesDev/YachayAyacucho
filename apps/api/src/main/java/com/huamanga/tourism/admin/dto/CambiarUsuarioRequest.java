package com.huamanga.tourism.admin.dto;

import com.huamanga.tourism.usuario.domain.EstadoUsuario;
import com.huamanga.tourism.usuario.domain.NombreRol;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cambio de rol o de estado de una cuenta (RF-51).
 *
 * <p>Los dos campos son opcionales por separado: se puede suspender sin tocar el
 * rol y al reves. Enviar ambos vacios no hace nada.</p>
 *
 * <p>No admite contrasena a proposito. Un administrador no cambia la clave de
 * nadie: si alguien pierde la suya, el camino es el de recuperacion por correo,
 * no que otra persona le elija una que despues conoce.</p>
 */
@Schema(description = "Nuevo rol y/o estado de una cuenta")
public record CambiarUsuarioRequest(

        @Schema(description = "Rol nuevo; se omite para no cambiarlo")
        NombreRol rol,

        @Schema(description = "Estado nuevo; se omite para no cambiarlo")
        EstadoUsuario estado
) {
}
