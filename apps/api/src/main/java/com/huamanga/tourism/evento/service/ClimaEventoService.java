package com.huamanga.tourism.evento.service;

import com.huamanga.tourism.clima.dto.PronosticoResponse;
import com.huamanga.tourism.clima.service.ClimaService;
import com.huamanga.tourism.common.tiempo.TiempoAyacucho;
import com.huamanga.tourism.evento.domain.Temporada;
import com.huamanga.tourism.evento.dto.ClimaEventoResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Decide que clima se puede decir de un evento y cual no (RF-88).
 *
 * <p>El pronostico gratuito alcanza cinco dias. Casi todos los eventos de una
 * agenda cultural estan mas lejos, asi que <strong>no tener pronostico es lo
 * habitual</strong>. Este servicio existe para que eso no se traduzca nunca en
 * un error: convierte "todavia no hay dato" en una respuesta con sentido.</p>
 *
 * <p>Ni una sola rama devuelve un fallo. Un evento sin pronostico informa de la
 * temporada; uno pasado no muestra clima; y si el proveedor se cae, se dice que
 * no esta disponible, que es distinto de que no exista.</p>
 */
@Service
public class ClimaEventoService {

    private final ClimaService climaService;
    private final Clock clock;

    public ClimaEventoService(ClimaService climaService, Clock clock) {
        this.climaService = climaService;
        this.clock = clock;
    }

    /** Clima de un evento, resolviendo el pronostico por su cuenta. */
    public ClimaEventoResponse paraEvento(LocalDate fechaInicio, LocalDate fechaFin) {
        return resolver(climaService.pronostico(), fechaInicio, fechaFin, TiempoAyacucho.hoy(clock));
    }

    /**
     * Clima de varios dias sueltos, para "Durante mi visita" (RF-84b).
     *
     * <p>Pide el pronostico <strong>una sola vez</strong> y lo reparte. Un viaje
     * de treinta dias resuelto dia a dia costaria treinta lecturas de Redis para
     * repartir exactamente el mismo objeto.</p>
     */
    public List<ClimaEventoResponse> paraDias(List<LocalDate> dias) {
        PronosticoResponse pronostico = climaService.pronostico();
        LocalDate hoy = TiempoAyacucho.hoy(clock);
        return dias.stream().map(dia -> resolver(pronostico, dia, dia, hoy)).toList();
    }

    private ClimaEventoResponse resolver(PronosticoResponse pronostico,
                                         LocalDate fechaInicio, LocalDate fechaFin, LocalDate hoy) {

        if (fechaFin.isBefore(hoy)) {
            return ClimaEventoResponse.pasado();
        }

        // De un evento que ya empezo interesa el tiempo de hoy, no el del dia en
        // que arranco: quien lo consulta esta decidiendo si acercarse ahora.
        LocalDate diaRelevante = fechaInicio.isBefore(hoy) ? hoy : fechaInicio;
        long faltan = ChronoUnit.DAYS.between(hoy, diaRelevante);
        Temporada temporada = Temporada.de(diaRelevante);

        if (!pronostico.disponible()) {
            return ClimaEventoResponse.noDisponible(temporada, faltan);
        }

        Optional<PronosticoResponse.PronosticoDia> dia = pronostico.dias().stream()
                .filter(candidato -> candidato.fecha().equals(diaRelevante))
                .findFirst();

        return dia.map(encontrado -> ClimaEventoResponse.conPronostico(encontrado, temporada, faltan))
                .orElseGet(() -> ClimaEventoResponse.fueraDeAlcance(temporada, faltan));
    }
}
