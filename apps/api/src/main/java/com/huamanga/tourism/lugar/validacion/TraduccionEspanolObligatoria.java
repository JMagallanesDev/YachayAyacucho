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
 * <p>Sirve para cualquier contenido traducible. Bean Validation elige el
 * validador cuyo tipo coincide con el objeto que se esta validando, asi que
 * sumar un contenido nuevo solo cuesta anadir su validador a esta lista, sin
 * duplicar la anotacion ni su mensaje traducido.</p>
 *
 * @see TraduccionEspanolObligatoriaValidador validador de lugares
 * @see com.huamanga.tourism.evento.validacion.TraduccionEventoValidador validador de eventos
 */
@Documented
@Constraint(validatedBy = {
        TraduccionEspanolObligatoriaValidador.class,
        com.huamanga.tourism.evento.validacion.TraduccionEventoValidador.class
})
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TraduccionEspanolObligatoria {

    String message() default "{traduccion.espanol.obligatoria}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
