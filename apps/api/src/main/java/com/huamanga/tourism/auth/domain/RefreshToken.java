package com.huamanga.tourism.auth.domain;

import com.huamanga.tourism.common.domain.EntidadCreacion;
import com.huamanga.tourism.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Token de refresco de sesion (7 dias).
 *
 * <p>Se guarda <strong>hasheado</strong>, nunca en claro: si alguien
 * consiguiera leer la tabla no podria reutilizar ninguna sesion. El token real
 * viaja en una cookie httpOnly + Secure + SameSite, inaccesible desde
 * JavaScript, y rota en cada uso (seccion 5.3 del plan).</p>
 *
 * <p>Solo lleva {@code created_at}: es un hecho inmutable. Renovar una sesion
 * no modifica la fila, crea otra y borra la anterior.</p>
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends EntidadCreacion {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    public boolean estaExpirado(Instant ahora) {
        return expiraEn.isBefore(ahora);
    }
}
