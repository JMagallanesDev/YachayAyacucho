package com.huamanga.tourism.evento.validacion;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.evento.dto.EventoRequest;
import com.huamanga.tourism.evento.dto.EventoTraduccionRequest;
import com.huamanga.tourism.lugar.validacion.TraduccionEspanolObligatoria;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.Set;

/**
 * La misma regla de los lugares, aplicada a los eventos: el espanol es
 * obligatorio y no puede haber dos traducciones al mismo idioma.
 *
 * <p>No se duplica la anotacion, solo el validador: Bean Validation admite
 * varios validadores por anotacion y elige el que corresponde al tipo que se
 * esta validando. Asi el mensaje traducido y la semantica viven en un unico
 * sitio, y anadir manana un tercer tipo con traducciones solo cuesta esta
 * clase.</p>
 */
public class TraduccionEventoValidador
        implements ConstraintValidator<TraduccionEspanolObligatoria, EventoRequest> {

    @Override
    public boolean isValid(EventoRequest peticion, ConstraintValidatorContext contexto) {
        if (peticion == null || peticion.traducciones() == null || peticion.traducciones().isEmpty()) {
            // La lista vacia ya la rechaza @NotEmpty en el propio campo.
            return true;
        }

        Set<Idioma> vistos = new HashSet<>();
        boolean tieneEspanol = false;

        for (EventoTraduccionRequest traduccion : peticion.traducciones()) {
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
