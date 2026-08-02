package com.huamanga.tourism.lugar.validacion;

import com.huamanga.tourism.lugar.dto.LugarRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Comprueba que un punto cae dentro del rectangulo que envuelve Ayacucho.
 */
public class DentroDeAyacuchoValidador implements ConstraintValidator<DentroDeAyacucho, LugarRequest> {

    /** Los mismos limites que el CHECK ck_lugar_bounds_ayacucho de la V4. */
    public static final double LONGITUD_MINIMA = -75.5;
    public static final double LONGITUD_MAXIMA = -73.0;
    public static final double LATITUD_MINIMA = -15.5;
    public static final double LATITUD_MAXIMA = -12.5;

    @Override
    public boolean isValid(LugarRequest peticion, ConstraintValidatorContext contexto) {
        if (peticion == null || peticion.longitud() == null || peticion.latitud() == null) {
            // La obligatoriedad la comprueban las anotaciones de cada campo;
            // aqui solo se valida el par cuando ambos existen.
            return true;
        }

        boolean dentro = peticion.longitud() >= LONGITUD_MINIMA
                && peticion.longitud() <= LONGITUD_MAXIMA
                && peticion.latitud() >= LATITUD_MINIMA
                && peticion.latitud() <= LATITUD_MAXIMA;

        if (!dentro) {
            // Se ancla el error al campo para que el formulario pueda
            // senalarlo, en vez de dejarlo como error global del objeto.
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(contexto.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("longitud")
                    .addConstraintViolation();
        }
        return dentro;
    }
}
