package com.huamanga.tourism.auth.dto;

import com.huamanga.tourism.usuario.domain.NombreRol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Datos publicos de un usuario.
 *
 * <p>No existe ningun campo para {@code passwordHash} y es deliberado: al no
 * estar en el record, es imposible que se filtre en una respuesta por
 * descuido. Es la razon por la que el CLAUDE.md prohibe devolver entidades
 * JPA desde los controllers.</p>
 */
@Schema(description = "Datos publicos de un usuario")
public record UsuarioResponse(
        UUID id,
        String email,
        String nombre,
        NombreRol rol,
        Instant registradoEn
) {
}
