package com.huamanga.tourism.foto.domain;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Foto de un lugar subida por un usuario.
 *
 * <p>El binario vive en Cloudinary; aqui solo se guardan la URL y el
 * {@code public_id}, que es lo que permite borrarla del CDN cuando se
 * rechaza.</p>
 */
@Entity
@Table(name = "foto")
@Getter
@Setter
@NoArgsConstructor
public class Foto extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lugar_id", nullable = false)
    private Lugar lugar;

    @Column(name = "cloudinary_url", nullable = false, length = 500)
    private String cloudinaryUrl;

    @Column(name = "cloudinary_public_id", nullable = false, length = 200)
    private String cloudinaryPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoFoto estado = EstadoFoto.PENDIENTE;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;
}
