package com.huamanga.tourism.lugar.validacion;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * La grilla semanal es coherente y sin franjas solapadas.
 *
 * @see HorariosCoherentesValidador
 */
@Documented
@Constraint(validatedBy = HorariosCoherentesValidador.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HorariosCoherentes {

    String message() default "{horario.rango.invalido}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
