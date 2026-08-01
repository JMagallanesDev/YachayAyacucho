package com.huamanga.tourism.common.domain;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Marca una clave primaria para que Hibernate la genere como UUID v7.
 *
 * <p>Se usa {@code @UuidV7} en lugar de asignar el identificador en el
 * constructor por un motivo concreto: si la entidad naciera con el id ya
 * puesto, Spring Data la consideraria "no nueva" y ejecutaria un SELECT
 * antes de cada INSERT para averiguar si debe insertar o actualizar. Con un
 * generador de Hibernate el id se asigna al persistir y ese SELECT extra
 * desaparece.</p>
 */
@IdGeneratorType(GeneradorUuidV7.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, METHOD})
public @interface UuidV7 {
}
