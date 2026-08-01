package com.huamanga.tourism.insignia.domain;

import com.huamanga.tourism.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Insignia obtenida por un usuario.
 *
 * <p>Solo se persiste el hecho inmutable —que la gano y cuando—, nunca el
 * progreso hacia ella. El progreso es un atributo derivado y almacenarlo
 * violaria 3FN: se calcula con un COUNT sobre check_in x lugar_ruta cuando
 * hace falta (seccion 6.6).</p>
 *
 * <p>La PK compuesta garantiza que nadie pueda ganar dos veces la misma
 * insignia, sin necesidad de un UNIQUE adicional.</p>
 */
@Entity
@Table(name = "insignia_usuario")
@Getter
@Setter
@NoArgsConstructor
public class InsigniaUsuario {

    @EmbeddedId
    private InsigniaUsuarioId id;

    @MapsId("usuarioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @MapsId("insigniaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insignia_id", nullable = false)
    private Insignia insignia;

    @Column(name = "obtenida_en", nullable = false)
    private Instant obtenidaEn;

    public InsigniaUsuario(Usuario usuario, Insignia insignia, Instant obtenidaEn) {
        this.usuario = usuario;
        this.insignia = insignia;
        this.obtenidaEn = obtenidaEn;
        this.id = new InsigniaUsuarioId(usuario.getId(), insignia.getId());
    }
}
