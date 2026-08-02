package com.huamanga.tourism.lugar.validacion;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.lugar.dto.LugarRequest;
import com.huamanga.tourism.lugar.dto.LugarTraduccionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.Set;

/**
 * El espanol es obligatorio; el ingles, opcional.
 *
 * <p>El espanol es la lengua base del contenido patrimonial y el destino del
 * fallback cuando falta una traduccion. Sin el, una ficha podria quedar sin
 * ningun texto que mostrar.</p>
 *
 * <p>De paso rechaza dos traducciones al mismo idioma: la clave primaria
 * compuesta de la tabla lo impediria, pero fallando con un error de
 * integridad en vez de con un mensaje comprensible.</p>
 */
public class TraduccionEspanolObligatoriaValidador
        implements ConstraintValidator<TraduccionEspanolObligatoria, LugarRequest> {

    @Override
    public boolean isValid(LugarRequest peticion, ConstraintValidatorContext contexto) {
        if (peticion == null || peticion.traducciones().isEmpty()) {
            // La lista vacia ya la rechaza @NotEmpty en el propio campo.
            return true;
        }

        Set<Idioma> vistos = new HashSet<>();
        boolean tieneEspanol = false;

        for (LugarTraduccionRequest traduccion : peticion.traducciones()) {
            if (traduccion.idioma() == null) {
                continue;
            }
            if (!vistos.add(traduccion.idioma())) {
                return rechazar(contexto, "{traduccion.idioma.duplicado}");
            }
            if (traduccion.idioma() == Idioma.ES) {
                tieneEspanol = true;
            }
        }

        return tieneEspanol || rechazar(contexto, "{traduccion.espanol.obligatoria}");
    }

    private boolean rechazar(ConstraintValidatorContext contexto, String plantilla) {
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(plantilla)
                .addPropertyNode("traducciones")
                .addConstraintViolation();
        return false;
    }
}
