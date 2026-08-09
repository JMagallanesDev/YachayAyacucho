package com.huamanga.tourism.evento.validacion;

import com.huamanga.tourism.evento.dto.EventoRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Comprueba que el evento no termine antes de empezar.
 *
 * <p>Tambien pone un techo a la duracion: una "fiesta" de mas de un mes casi
 * siempre es un error de tecleo en el anio, y publicarla llenaria el calendario
 * entero de un mismo evento.</p>
 */
public class FechasCoherentesValidador
        implements ConstraintValidator<FechasCoherentes, EventoRequest> {

    /** Ninguna festividad de la agenda dura mas de un mes. */
    private static final long MAXIMO_DIAS = 31;

    @Override
    public boolean isValid(EventoRequest peticion, ConstraintValidatorContext contexto) {
        if (peticion == null || peticion.fechaInicio() == null || peticion.fechaFin() == null) {
            // Los @NotNull de los campos ya se encargan de esto.
            return true;
        }

        if (peticion.fechaFin().isBefore(peticion.fechaInicio())) {
            return rechazar(contexto, "{evento.fechas.invertidas}");
        }

        long dias = java.time.temporal.ChronoUnit.DAYS
                .between(peticion.fechaInicio(), peticion.fechaFin()) + 1;

        return dias <= MAXIMO_DIAS || rechazar(contexto, "{evento.fechas.demasiado.largas}");
    }

    private boolean rechazar(ConstraintValidatorContext contexto, String plantilla) {
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(plantilla)
                .addPropertyNode("fechaFin")
                .addConstraintViolation();
        return false;
    }
}
