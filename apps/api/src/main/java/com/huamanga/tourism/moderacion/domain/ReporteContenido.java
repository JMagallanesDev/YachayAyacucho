package com.huamanga.tourism.moderacion.domain;

import com.huamanga.tourism.common.domain.EntidadCreacion;
import com.huamanga.tourism.foto.domain.Foto;
import com.huamanga.tourism.resena.domain.Resena;
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
 * Denuncia de un usuario contra una foto o una resena (RF-45).
 *
 * <p>Dos FK nullables con un CHECK que exige exactamente una, en lugar de una
 * FK polimorfica. La razon es de integridad: PostgreSQL no puede validar una
 * columna que apunte a dos tablas distintas, mientras que con dos FK reales se
 * conserva el ON DELETE CASCADE nativo. La FK polimorfica es un anti-patron
 * documentado (seccion 6.6).</p>
 *
 * <p>Al tercer reporte distinto sobre el mismo contenido, su estado pasa a
 * EN_REVISION. El UNIQUE parcial impide que un mismo usuario cuente tres
 * veces.</p>
 */
@Entity
@Table(name = "reporte_contenido")
@Getter
@Setter
@NoArgsConstructor
public class ReporteContenido extends EntidadCreacion {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foto_id")
    private Foto foto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resena_id")
    private Resena resena;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 30)
    private MotivoReporteContenido motivo;

    /** Comprueba en Java el mismo XOR que garantiza el CHECK de la BD. */
    public boolean apuntaAUnSoloContenido() {
        return (foto != null) ^ (resena != null);
    }
}
