package com.huamanga.tourism.favorito.domain;

import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Lugar marcado como favorito por un usuario (RF-35).
 *
 * <p>Pivote N:M puro: sin clave subrogada, porque la PK compuesta ya impide
 * duplicados. Anadir una columna {@code id} obligaria a un UNIQUE extra para
 * lograr exactamente lo mismo.</p>
 */
@Entity
@Table(name = "favorito")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Favorito {

    @EmbeddedId
    private FavoritoId id;

    @MapsId("usuarioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @MapsId("lugarId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lugar_id", nullable = false)
    private Lugar lugar;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Favorito(Usuario usuario, Lugar lugar) {
        this.usuario = usuario;
        this.lugar = lugar;
        this.id = new FavoritoId(usuario.getId(), lugar.getId());
    }
}
