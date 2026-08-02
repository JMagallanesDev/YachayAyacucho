package com.huamanga.tourism.lugar.validacion;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige traduccion al espanol y prohibe idiomas repetidos.
 *
 * @see TraduccionEspanolObligatoriaValidador
 */
@Documented
@Constraint(validatedBy = TraduccionEspanolObligatoriaValidador.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TraduccionEspanolObligatoria {

    String message() default "{traduccion.espanol.obligatoria}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
