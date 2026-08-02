package com.huamanga.tourism.lugar.validacion;

import com.huamanga.tourism.lugar.dto.HorarioRequest;
import com.huamanga.tourism.lugar.dto.LugarRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

/**
 * Valida la grilla semanal completa.
 *
 * <p>Dos reglas:</p>
 * <ol>
 *   <li>Cada franja es coherente: o el dia esta cerrado, o tiene apertura
 *       anterior al cierre.</li>
 *   <li><strong>Ninguna franja se solapa con otra del mismo dia.</strong> No
 *       lo pide el plan, pero sin esta comprobacion se podria guardar
 *       "9:00-13:00" y "12:00-18:00" el mismo martes, y el calculo de
 *       "abierto ahora" (RF-09b) quedaria respondiendo sobre datos
 *       contradictorios.</li>
 * </ol>
 */
public class HorariosCoherentesValidador
        implements ConstraintValidator<HorariosCoherentes, LugarRequest> {

    @Override
    public boolean isValid(LugarRequest peticion, ConstraintValidatorContext contexto) {
        if (peticion == null || peticion.horarios().isEmpty()) {
            return true;
        }

        List<HorarioRequest> horarios = peticion.horarios();

        for (HorarioRequest horario : horarios) {
            if (!horario.esCoherente()) {
                return rechazar(contexto, "{horario.rango.invalido}");
            }
        }

        for (int i = 0; i < horarios.size(); i++) {
            for (int j = i + 1; j < horarios.size(); j++) {
                if (horarios.get(i).seSolapaCon(horarios.get(j))) {
                    return rechazar(contexto, "{horario.solapado}");
                }
            }
        }

        return true;
    }

    private boolean rechazar(ConstraintValidatorContext contexto, String plantilla) {
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(plantilla)
                .addPropertyNode("horarios")
                .addConstraintViolation();
        return false;
    }
}
