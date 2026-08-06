package com.huamanga.tourism.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import java.time.Instant;
import java.util.UUID;

/**
 * Base de las entidades de contenido gestionadas desde el panel de
 * administracion: registran quien las creo, quien las modifico por ultima vez
 * y si fueron dadas de baja.
 *
 * <p>Aqui vive el soft delete. Un lugar patrimonial, un evento o un reporte
 * ciudadano no se borran fisicamente: pierden visibilidad pero se conservan
 * para la auditoria (RF-56) y para poder revertir un error de moderacion. Las
 * entidades que lo usan lo combinan con {@code @SQLRestriction("deleted_at IS
 * NULL")}, de modo que lo eliminado desaparece de las consultas sin que cada
 * repositorio tenga que acordarse de filtrarlo.</p>
 *
 * <p>Quien la extiende: Lugar, Evento, RutaTematica, Negocio y Reporte.
 * Usuario tambien tiene soft delete, pero no columnas de autoria: un usuario
 * se registra a si mismo.</p>
 */
@MappedSuperclass
public abstract class EntidadAuditable extends EntidadBase {

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    /**
     * Borra la huella de auditoria de esta fila.
     *
     * <p>Existe por una sola razon, y conviene que siga siendo asi: los
     * <strong>reportes ciudadanos anonimos</strong> (RF-72). La auditoria
     * rellena {@code created_by} desde el SecurityContext, de modo que alguien
     * con sesion iniciada que pide anonimato acabaria identificado en esa
     * columna. Vease {@code Reporte.borrarRastroSiEsAnonimo()}.</p>
     *
     * <p>Es {@code protected} a proposito: solo una subclase puede renunciar a
     * su propia trazabilidad, y debe hacerlo de forma explicita. Ningun service
     * puede borrar la auditoria de una entidad desde fuera.</p>
     */
    protected void borrarAuditoria() {
        this.createdBy = null;
        this.updatedBy = null;
    }

    /** Marca la fila como eliminada sin borrarla. */
    public void eliminar(Instant momento) {
        this.deletedAt = momento;
    }

    /** Revierte una eliminacion logica. */
    public void restaurar() {
        this.deletedAt = null;
    }

    public boolean estaEliminado() {
        return deletedAt != null;
    }
}
