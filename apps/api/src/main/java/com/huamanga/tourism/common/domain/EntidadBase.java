package com.huamanga.tourism.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/**
 * Base de las entidades que se crean y se modifican.
 *
 * <p>Es el caso general: catalogos, contenido de usuarios y tablas de
 * hechos agregados.</p>
 */
@MappedSuperclass
public abstract class EntidadBase extends EntidadCreacion {

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    protected void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
