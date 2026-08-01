package com.huamanga.tourism.negocio.domain;

import com.huamanga.tourism.common.domain.EntidadAuditable;
import com.huamanga.tourism.geografia.domain.Distrito;
import com.huamanga.tourism.usuario.domain.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.locationtech.jts.geom.Point;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Negocio local del directorio (Modulo 13).
 *
 * <p>{@code horarioTexto} es texto libre a proposito, y no una tabla
 * normalizada como {@code horario_lugar}. No es una inconsistencia sino una
 * decision basada en el uso del dato: el horario del lugar patrimonial
 * alimenta logica computable (abierto ahora, recomendaciones, planificador);
 * el del negocio solo se muestra. Normalizarlo seria anadir una tabla, sus
 * constraints y su CRUD para una funcionalidad que no existe en el alcance
 * (seccion 6.6).</p>
 *
 * <p>La descripcion vive en {@link NegocioTraduccion}, no aqui.</p>
 */
@Entity
@Table(name = "negocio")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Negocio extends EntidadAuditable {

    /** Usuario con rol NEGOCIO que gestiona el perfil. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_negocio_id", nullable = false)
    private CategoriaNegocio categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distrito_id", nullable = false)
    private Distrito distrito;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "telefono", length = 30)
    private String telefono;

    /** Numero para el boton de contacto directo (RF-110). */
    @Column(name = "whatsapp", length = 30)
    private String whatsapp;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "ubicacion", columnDefinition = "geometry(Point,4326)")
    private Point ubicacion;

    @Column(name = "horario_texto", length = 255)
    private String horarioTexto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoNegocio estado = EstadoNegocio.PENDIENTE;

    @OneToMany(mappedBy = "negocio", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NegocioTraduccion> traducciones = new LinkedHashSet<>();
}
