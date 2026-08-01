package com.huamanga.tourism.usuario.domain;

import com.huamanga.tourism.common.domain.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * Usuario del sistema.
 *
 * <p>Tiene soft delete pero no columnas de autoria: un usuario se registra a
 * si mismo, no lo crea un administrador. Por eso extiende
 * {@code EntidadBase} y declara {@code deletedAt} por su cuenta en lugar de
 * heredar de {@code EntidadAuditable}.</p>
 *
 * <p>{@code passwordHash} nunca sale de esta capa: los controllers devuelven
 * DTOs y este campo no se mapea a ninguno.</p>
 */
@Entity
@Table(name = "usuario")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Usuario extends EntidadBase {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void eliminar(Instant momento) {
        this.deletedAt = momento;
    }

    public boolean estaEliminado() {
        return deletedAt != null;
    }
}
