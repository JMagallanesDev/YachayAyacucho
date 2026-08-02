package com.huamanga.tourism.auth.repository;

import com.huamanga.tourism.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Trae el token con su usuario y el rol ya cargados.
     *
     * <p>Imprescindible al rotar: la respuesta se construye fuera de la
     * transaccion del service, y con {@code open-in-view=false} un proxy
     * perezoso reventaria alli con LazyInitializationException. Se piden en la
     * misma consulta las dos asociaciones que hacen falta —el usuario y su
     * rol, que va dentro del JWT— en lugar de tres viajes a la base.</p>
     */
    @Query("""
            SELECT t FROM RefreshToken t
            JOIN FETCH t.usuario u
            JOIN FETCH u.rol
            WHERE t.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashConUsuario(String tokenHash);

    void deleteByUsuarioId(UUID usuarioId);

    /** Limpieza programada de tokens caducados; usa idx_refresh_expira. */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiraEn < :momento")
    int eliminarCaducados(Instant momento);

    /**
     * Revoca de golpe todas las sesiones vivas de un usuario.
     *
     * <p>Se usa en el logout global y, sobre todo, cuando se detecta la
     * reutilizacion de un refresh token: ante un robo confirmado no sirve
     * invalidar solo el token presentado, hay que cortar todas las sesiones.</p>
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken t SET t.revocadoEn = :momento
            WHERE t.usuario.id = :usuarioId
              AND t.revocadoEn IS NULL
            """)
    int revocarTodosDelUsuario(UUID usuarioId, Instant momento);

    long countByUsuarioIdAndRevocadoEnIsNullAndUsadoEnIsNull(UUID usuarioId);
}
