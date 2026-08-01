package com.huamanga.tourism.resena.domain;

import com.huamanga.tourism.common.domain.EntidadBase;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resena de un usuario sobre un lugar patrimonial (RF-37).
 *
 * <p>El UNIQUE (usuario, lugar) no es un detalle: sin el, un mismo usuario
 * podria inflar la calificacion de un lugar publicando resenas repetidas.</p>
 */
@Entity
@Table(
        name = "resena",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resena_usuario_lugar",
                columnNames = {"usuario_id", "lugar_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class Resena extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lugar_id", nullable = false)
    private Lugar lugar;

    /** De 1 a 5. La BD lo comprueba con un CHECK. */
    @Column(name = "calificacion", nullable = false)
    private Short calificacion;

    @Column(name = "comentario", length = 500)
    private String comentario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoResena estado = EstadoResena.PUBLICADA;
}
