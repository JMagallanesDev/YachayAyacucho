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

    void deleteByUsuarioId(UUID usuarioId);

    /** Limpieza programada de tokens caducados; usa idx_refresh_expira. */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiraEn < :momento")
    int eliminarCaducados(Instant momento);
}
