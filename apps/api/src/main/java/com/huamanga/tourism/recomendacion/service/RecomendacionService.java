package com.huamanga.tourism.recomendacion.service;

import com.huamanga.tourism.clima.dto.ClimaResponse;
import com.huamanga.tourism.clima.dto.PronosticoResponse;
import com.huamanga.tourism.clima.service.ClimaService;
import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.lugar.dto.LugarResumenResponse;
import com.huamanga.tourism.lugar.service.LugarService;
import com.huamanga.tourism.recomendacion.dto.RecomendacionResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * «¿Que hago ahora?» (RF-08).
 *
 * <p>Cruza cuatro senales —si el lugar esta abierto, el clima, la hora y la
 * categoria— y devuelve una lista corta y ordenada, con los motivos a la
 * vista.</p>
 *
 * <p>Que el clima no este disponible <strong>no anula la recomendacion</strong>:
 * se recomienda igual, solo que sin puntuar por clima. Un motor que se apaga
 * porque un servicio externo no responde es peor que uno que sabe funcionar con
 * menos informacion.</p>
 */
@Service
public class RecomendacionService {

    /** Cuantos lugares se examinan. Con 15 sobra; deja aire para crecer. */
    private static final int CANDIDATOS = 100;

    private static final int MAXIMO_SUGERENCIAS = 6;

    private final LugarService lugarService;
    private final ClimaService climaService;
    private final ReglasRecomendacion reglas;
    private final Clock clock;

    public RecomendacionService(LugarService lugarService, ClimaService climaService,
                                ReglasRecomendacion reglas, Clock clock) {
        this.lugarService = lugarService;
        this.climaService = climaService;
        this.reglas = reglas;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RecomendacionResponse> ahora(Idioma idioma) {
        ClimaResponse clima = climaService.actual();
        LocalTime hora = LocalDateTime.ofInstant(clock.instant(), ClimaService.ZONA_AYACUCHO)
                .toLocalTime();

        // Un clima obsoleto no se usa para decidir. Sirve para informar («hace
        // 2 h llovia»), pero mandar a alguien a un museo por una lluvia que
        // quiza escampo hace rato seria decidir con datos caducados.
        boolean climaFiable = clima.disponible() && !clima.obsoleto();

        return puntuar(idioma, clima.condicion(), hora, climaFiable, this::estaAbiertoAhora);
    }

    /**
     * Planificador por fecha (RF-29).
     *
     * <p>Si la fecha queda fuera del pronostico se responde igualmente, usando
     * solo los horarios. Devolver un error por consultar el mes que viene seria
     * hostil para algo tan corriente.</p>
     */
    @Transactional(readOnly = true)
    public Planificacion planificar(LocalDate fecha, Idioma idioma) {
        Optional<PronosticoResponse.PronosticoDia> dia = climaService.paraFecha(fecha);

        // Se evalua a media manana, que es cuando la mayoria empieza a visitar.
        LocalTime horaTipica = LocalTime.of(10, 0);

        List<RecomendacionResponse> sugerencias = puntuar(
                idioma,
                dia.map(PronosticoResponse.PronosticoDia::condicion).orElse(null),
                horaTipica,
                dia.isPresent(),
                lugar -> abreEn(lugar, fecha, horaTipica));

        return new Planificacion(fecha, dia.orElse(null), dia.isPresent(), sugerencias);
    }

    private List<RecomendacionResponse> puntuar(Idioma idioma, String condicion, LocalTime hora,
                                                boolean climaFiable,
                                                java.util.function.Predicate<LugarResumenResponse> abierto) {

        List<LugarResumenResponse> lugares = lugarService.explorar(
                        new LugarService.CriteriosBusqueda(null, null, null, null),
                        idioma,
                        PageRequest.of(0, CANDIDATOS))
                .getContent();

        return lugares.stream()
                .map(lugar -> {
                    var resultado = reglas.evaluar(lugar, abierto.test(lugar), condicion, hora, climaFiable);
                    return resultado.descartado()
                            ? null
                            : new RecomendacionResponse(lugar, resultado.puntuacion(), resultado.motivos());
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(RecomendacionResponse::puntuacion).reversed()
                        // Desempate estable por nombre: sin el, dos lugares con
                        // la misma puntuacion cambiarian de orden entre
                        // peticiones y la lista «bailaria» al recargar.
                        .thenComparing(r -> r.lugar().nombre()))
                .limit(MAXIMO_SUGERENCIAS)
                .toList();
    }

    private boolean estaAbiertoAhora(LugarResumenResponse lugar) {
        LocalDateTime ahora = LocalDateTime.ofInstant(clock.instant(), ClimaService.ZONA_AYACUCHO);
        return abreEn(lugar, ahora.toLocalDate(), ahora.toLocalTime());
    }

    /** DayOfWeek va de 1 (lunes) a 7 (domingo); el modelo usa 0 = domingo. */
    private boolean abreEn(LugarResumenResponse lugar, LocalDate fecha, LocalTime hora) {
        short dia = (short) (fecha.getDayOfWeek().getValue() % 7);

        return lugar.horarios().stream()
                .filter(horario -> horario.diaSemana() == dia && !horario.cerrado())
                .anyMatch(horario -> horario.horaApertura() != null
                        && horario.horaCierre() != null
                        && !hora.isBefore(horario.horaApertura())
                        && hora.isBefore(horario.horaCierre()));
    }

    /**
     * @param pronosticoDisponible false cuando la fecha cae fuera del alcance
     *                             del pronostico; las sugerencias siguen siendo
     *                             validas, solo que basadas unicamente en horarios
     */
    public record Planificacion(
            LocalDate fecha,
            PronosticoResponse.PronosticoDia pronostico,
            boolean pronosticoDisponible,
            List<RecomendacionResponse> sugerencias) {
    }
}
