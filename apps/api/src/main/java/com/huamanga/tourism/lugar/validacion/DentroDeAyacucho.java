package com.huamanga.tourism.lugar.validacion;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que las coordenadas caigan dentro de la region Ayacucho (RF-22b).
 *
 * <p>Va a nivel de clase porque necesita longitud y latitud a la vez.</p>
 *
 * <p>La base de datos ya lo impide con un CHECK, pero una violacion de CHECK
 * llega como error de integridad y se traduce en un 500 sin explicacion. Al
 * validarlo antes, el administrador recibe un 400 que dice exactamente que
 * pasa y en su idioma (RNF-23).</p>
 */
@Documented
@Constraint(validatedBy = DentroDeAyacuchoValidador.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DentroDeAyacucho {

    String message() default "{lugar.coordenadas.fuera}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
