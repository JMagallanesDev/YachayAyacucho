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

    /** Momento en que se roto. No nulo = ya se uso y no debe volver a servir. */
    @Column(name = "usado_en")
    private Instant usadoEn;

    /** Invalidado por logout o por revocacion en cascada tras un robo. */
    @Column(name = "revocado_en")
    private Instant revocadoEn;

    public boolean estaExpirado(Instant ahora) {
        return expiraEn.isBefore(ahora);
    }

    public boolean fueUsado() {
        return usadoEn != null;
    }

    public boolean estaRevocado() {
        return revocadoEn != null;
    }

    /** Solo es utilizable un token vivo: ni usado, ni revocado, ni caducado. */
    public boolean esUtilizable(Instant ahora) {
        return !fueUsado() && !estaRevocado() && !estaExpirado(ahora);
    }

    public void marcarUsado(Instant momento) {
        this.usadoEn = momento;
    }

    public void revocar(Instant momento) {
        this.revocadoEn = momento;
    }
}
