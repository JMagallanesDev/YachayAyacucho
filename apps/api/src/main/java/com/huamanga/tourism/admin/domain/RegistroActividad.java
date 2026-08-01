package com.huamanga.tourism.admin.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Log inmutable de acciones administrativas (RF-56).
 *
 * <p>Aqui la IP <strong>si</strong> se guarda, al contrario que en los
 * reportes ciudadanos. No es contradiccion: esto es auditoria interna de un
 * usuario identificado que ejerce privilegios sobre el contenido; aquello era
 * una denuncia anonima cuya proteccion es el objetivo.</p>
 *
 * <p>{@code detalles} es JSONB porque cada accion registra una estructura
 * distinta. Es un log, no datos relacionales de dominio: la BD nunca hace JOIN
 * ni consulta dentro del JSON, y por eso no compromete 1FN (seccion 6.6).</p>
 */
@Entity
@Table(name = "registro_actividad")
@Getter
@Setter
@NoArgsConstructor
public class RegistroActividad extends EntidadCreacion {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    /** Nombre de la entidad afectada, p. ej. "Lugar". */
    @Column(name = "entidad", nullable = false, length = 50)
    private String entidad;

    @Column(name = "entidad_id")
    private UUID entidadId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalles", columnDefinition = "jsonb")
    private String detalles;

    /** Admite IPv6, por eso 45 caracteres. */
    @Column(name = "ip", length = 45)
    private String ip;
}
