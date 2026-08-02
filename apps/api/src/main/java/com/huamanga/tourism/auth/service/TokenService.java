package com.huamanga.tourism.auth.service;

import com.huamanga.tourism.auth.config.PropiedadesSeguridad;
import com.huamanga.tourism.usuario.domain.Usuario;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Emite los access tokens JWT.
 *
 * <p>La validacion no vive aqui: la hace el resource server de Spring
 * Security con el {@code JwtDecoder} configurado en {@code SecurityConfig}.
 * Escribir esa parte a mano seria repetir codigo ya auditado en el punto mas
 * delicado del sistema.</p>
 */
@Service
public class TokenService {

    /** Claim con el rol. Se lee al construir las autoridades de Spring. */
    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_EMAIL = "email";

    private final JwtEncoder jwtEncoder;
    private final PropiedadesSeguridad propiedades;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, PropiedadesSeguridad propiedades, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    /**
     * Genera un access token para un usuario.
     *
     * <p>Los claims son deliberadamente escuetos: identificador, correo y rol.
     * Un JWT va firmado pero <strong>no cifrado</strong>, asi que cualquiera
     * que lo intercepte lee su contenido con un Base64. Nada que no pueda ser
     * publico entra aqui.</p>
     */
    public String generarAccessToken(Usuario usuario) {
        Instant ahora = clock.instant();
        Instant expira = ahora.plus(propiedades.accessExpiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.emisor())
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(usuario.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_EMAIL, usuario.getEmail())
                .claim(CLAIM_ROL, usuario.getRol().getNombre().name())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(() -> "HS256").build(), claims))
                .getTokenValue();
    }

    public long segundosDeVidaDelAccessToken() {
        return propiedades.accessExpiration().toSeconds();
    }
}
