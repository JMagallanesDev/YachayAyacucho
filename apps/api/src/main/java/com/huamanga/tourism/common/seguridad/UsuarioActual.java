package com.huamanga.tourism.common.seguridad;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * Lee el usuario autenticado del SecurityContext.
 *
 * <p>El identificador viaja en el claim {@code sub} del JWT, que Spring ya
 * verifico antes de llegar aqui: si hay un {@link Jwt} en el contexto, su
 * firma es valida y no ha caducado.</p>
 */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    /** Vacio cuando la peticion es anonima. */
    public static Optional<UUID> id() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            return Optional.empty();
        }
        if (autenticacion.getPrincipal() instanceof Jwt jwt) {
            try {
                return Optional.of(UUID.fromString(jwt.getSubject()));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static UUID idObligatorio() {
        return id().orElseThrow(() -> new IllegalStateException("No hay usuario autenticado"));
    }
}
