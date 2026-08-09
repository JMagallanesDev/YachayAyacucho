package com.huamanga.tourism.evento.validacion;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * La fecha de fin no puede ser anterior a la de inicio.
 *
 * <p>La tabla ya lo impide con un CHECK, pero un CHECK falla con un error de
 * integridad de PostgreSQL, no con un mensaje que un administrador entienda.
 * Esta anotacion lo convierte en un 400 con el texto traducido.</p>
 *
 * @see FechasCoherentesValidador
 */
@Documented
@Constraint(validatedBy = FechasCoherentesValidador.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FechasCoherentes {

    String message() default "{evento.fechas.invertidas}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
