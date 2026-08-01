package com.huamanga.tourism.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base de las entidades que solo registran cuando se crearon.
 *
 * <p>Son hechos inmutables del dominio —un check-in, un reporte de contenido,
 * una entrada del log— que nunca se modifican: registrar un
 * {@code updated_at} en ellas seria una columna que jamas cambia.</p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class EntidadCreacion {

    @Id
    @UuidV7
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    protected void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Igualdad por identificador.
     *
     * <p>Se compara con {@code Hibernate.getClass} y no con {@code getClass}
     * porque una entidad cargada de forma perezosa es en realidad un proxy, y
     * su clase real no es la de la entidad.</p>
     *
     * <p>Dos entidades sin persistir nunca son iguales: hasta que Hibernate no
     * asigna el id, no existe criterio de identidad.</p>
     */
    @Override
    public final boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (objeto == null || Hibernate.getClass(this) != Hibernate.getClass(objeto)) {
            return false;
        }
        EntidadCreacion otra = (EntidadCreacion) objeto;
        return id != null && id.equals(otra.id);
    }

    /**
     * Constante por clase a proposito: el hash de una entidad no puede cambiar
     * cuando Hibernate le asigna el id al persistirla, o dejaria de
     * encontrarse dentro de un HashSet en el que ya estaba.
     */
    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return Hibernate.getClass(this).getSimpleName() + "(id=" + id + ")";
    }
}
