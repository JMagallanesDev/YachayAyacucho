package com.huamanga.tourism.auth.service;

import com.huamanga.tourism.auth.config.PropiedadesSeguridad;
import com.huamanga.tourism.auth.domain.RefreshToken;
import com.huamanga.tourism.auth.exception.RefreshTokenInvalidoException;
import com.huamanga.tourism.auth.repository.RefreshTokenRepository;
import com.huamanga.tourism.usuario.domain.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Ciclo de vida del refresh token: emision, rotacion y revocacion.
 *
 * <p><strong>Por que SHA-256 y no BCrypt para guardarlo.</strong> BCrypt es
 * lento a proposito, porque protege secretos de baja entropia que un humano
 * elige. Un refresh token son 256 bits aleatorios: no hay diccionario ni
 * fuerza bruta que valga contra eso, asi que un hash rapido basta. Y hace
 * falta que sea rapido y determinista, porque el token se busca <em>por su
 * hash</em> en cada renovacion; con BCrypt habria que recorrer la tabla
 * entera comparando uno a uno.</p>
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** 32 bytes = 256 bits de entropia. */
    private static final int BYTES_TOKEN = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final PropiedadesSeguridad propiedades;
    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               PropiedadesSeguridad propiedades,
                               Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    /**
     * Emite un refresh token nuevo y devuelve su valor <strong>en claro</strong>,
     * que es lo unico que viaja al navegador. En la base solo queda el hash.
     */
    @Transactional
    public String emitir(Usuario usuario) {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        String tokenCrudo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setTokenHash(hashear(tokenCrudo));
        token.setExpiraEn(clock.instant().plus(propiedades.refreshExpiration()));
        refreshTokenRepository.save(token);

        return tokenCrudo;
    }

    /**
     * Rota un refresh token: invalida el presentado y emite otro.
     *
     * <p>Aqui vive la deteccion de robo. Si el token existe pero ya fue usado,
     * la unica explicacion posible es que dos actores tienen la misma
     * credencial: el legitimo, que ya roto, y alguien mas. Ante esa senal no
     * basta con rechazar la peticion —el ladron seguiria teniendo el token
     * actual—, hay que <strong>revocar todas las sesiones del usuario</strong>
     * y obligarle a autenticarse de nuevo.</p>
     *
     * <p><strong>{@code noRollbackFor} no es opcional aqui.</strong> Spring
     * deshace la transaccion ante cualquier RuntimeException, asi que sin esta
     * clausula la revocacion en cascada se escribiria y acto seguido el
     * rollback la borraria: el sistema detectaria el robo y no haria nada al
     * respecto. La excepcion es una senal de negocio —"este token no vale"—,
     * no un fallo que deba anular el trabajo ya hecho.</p>
     *
     * @return el usuario y el nuevo token en claro
     */
    @Transactional(noRollbackFor = RefreshTokenInvalidoException.class)
    public ResultadoRotacion rotar(String tokenCrudo) {
        Instant ahora = clock.instant();
        RefreshToken token = refreshTokenRepository.findByTokenHashConUsuario(hashear(tokenCrudo))
                .orElseThrow(() -> new RefreshTokenInvalidoException("Refresh token desconocido"));

        if (token.fueUsado()) {
            UUID usuarioId = token.getUsuario().getId();
            int revocados = refreshTokenRepository.revocarTodosDelUsuario(usuarioId, ahora);
            log.warn("Reutilizacion de refresh token detectada para el usuario {}. "
                    + "Se revocaron {} sesiones activas.", usuarioId, revocados);
            throw new RefreshTokenInvalidoException("Refresh token ya utilizado");
        }

        if (!token.esUtilizable(ahora)) {
            throw new RefreshTokenInvalidoException("Refresh token caducado o revocado");
        }

        token.marcarUsado(ahora);
        Usuario usuario = token.getUsuario();
        return new ResultadoRotacion(usuario, emitir(usuario));
    }

    /** Cierra una sesion concreta. Un token desconocido no es un error. */
    @Transactional
    public void cerrarSesion(String tokenCrudo) {
        refreshTokenRepository.findByTokenHash(hashear(tokenCrudo))
                .ifPresent(token -> token.revocar(clock.instant()));
    }

    @Transactional
    public int revocarTodasLasSesiones(UUID usuarioId) {
        return refreshTokenRepository.revocarTodosDelUsuario(usuarioId, clock.instant());
    }

    /**
     * SHA-256 en hexadecimal. Determinista, por eso se puede indexar y buscar.
     */
    private String hashear(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hexadecimal.append(Character.forDigit((b >> 4) & 0xF, 16));
                hexadecimal.append(Character.forDigit(b & 0xF, 16));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 es obligatorio en toda JVM; si falta, algo va muy mal.
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", ex);
        }
    }

    /** Resultado de una rotacion: a quien pertenece y cual es el token nuevo. */
    public record ResultadoRotacion(Usuario usuario, String tokenCrudo) {
    }
}
