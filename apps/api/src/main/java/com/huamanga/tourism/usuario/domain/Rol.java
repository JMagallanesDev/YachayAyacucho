package com.huamanga.tourism.usuario.domain;

import com.huamanga.tourism.common.domain.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rol del sistema. Se siembra en la migracion V14 con los 4 valores.
 */
@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
public class Rol extends EntidadBase {

    // STRING y no ORDINAL: con ORDINAL, reordenar el enum reasignaria
    // silenciosamente los roles de todos los usuarios ya guardados.
    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", nullable = false, unique = true, length = 20)
    private NombreRol nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;
}
