package com.huamanga.tourism.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base de las 8 tablas de traduccion.
 *
 * <p>No hereda de {@link EntidadCreacion} porque estas tablas no tienen clave
 * subrogada: su clave primaria es compuesta ({@code entidad_id + idioma}), que
 * es justo lo que impide tener dos traducciones del mismo registro al mismo
 * idioma sin necesidad de un UNIQUE adicional.</p>
 *
 * <p>Este patron es el que permite que anadir un idioma sea un INSERT y no un
 * ALTER TABLE.</p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class MarcaTiempoTraduccion {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
