package com.huamanga.tourism.admin.dto;

import com.huamanga.tourism.usuario.domain.EstadoUsuario;
import com.huamanga.tourism.usuario.domain.NombreRol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Un usuario visto desde el panel (RF-51).
 *
 * <p><strong>Este record no tiene ningun campo de contrasena, y esa ausencia es
 * deliberada.</strong> No hay {@code password}, ni {@code passwordHash}, ni un
 * campo «oculto» que viaje vacio: el dato no entra en el DTO, asi que no puede
 * salir por descuido en una respuesta, en un log ni en la consola del navegador.
 * Es la razon por la que el panel nunca serializa la entidad {@code Usuario}
 * directamente, que si lo tiene.</p>
 *
 * <p>Un test comprueba que la respuesta del listado no contiene la cadena
 * {@code $2a$} —el prefijo de BCrypt— en ningun sitio.</p>
 */
@Schema(description = "Usuario para la gestion del panel, sin datos de credenciales")
public record UsuarioAdminResponse(

        UUID id,

        String email,

        String nombre,

        NombreRol rol,

        EstadoUsuario estado,

        @Schema(description = "Cuando se registro")
        Instant registradoEn,

        @Schema(description = "true si es la cuenta con la que se esta administrando ahora")
        boolean esTuCuenta
) {
}
