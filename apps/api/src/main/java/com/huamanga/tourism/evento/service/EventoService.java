package com.huamanga.tourism.evento.service;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.tiempo.TiempoAyacucho;
import com.huamanga.tourism.evento.domain.EstadoEvento;
import com.huamanga.tourism.evento.domain.Evento;
import com.huamanga.tourism.evento.domain.TipoEvento;
import com.huamanga.tourism.evento.dto.EventoDetalleResponse;
import com.huamanga.tourism.evento.dto.EventoResumenResponse;
import com.huamanga.tourism.evento.dto.VisitaResponse;
import com.huamanga.tourism.evento.mapper.EventoMapper;
import com.huamanga.tourism.evento.repository.EventoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Consulta publica de la agenda cultural (RF-79, RF-80, RF-84, RF-84b, RF-85).
 *
 * <p>Todas las lecturas publicas filtran por {@code PUBLICADO}: un borrador es
 * justamente lo que aun no debe verse, y un evento cancelado no puede seguir
 * anunciandose como si fuera a ocurrir.</p>
 */
@Service
public class EventoService {

    /** Techo del rango de un viaje, para que nadie pida el siglo entero. */
    public static final int MAXIMO_DIAS_DE_VIAJE = 30;

    private final EventoRepository eventoRepository;
    private final ClimaEventoService climaEventos;
    private final EventoMapper mapper;
    private final Clock clock;

    public EventoService(EventoRepository eventoRepository,
                         ClimaEventoService climaEventos,
                         EventoMapper mapper,
                         Clock clock) {
        this.eventoRepository = eventoRepository;
        this.climaEventos = climaEventos;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Eventos que tocan un rango de fechas. Alimenta el calendario mensual.
     *
     * <p>Es una consulta de solape, asi que una festividad de varios dias sale
     * en todos los meses que atraviesa. El frontend la reparte en sus dias.</p>
     */
    @Transactional(readOnly = true)
    public List<EventoResumenResponse> enRango(LocalDate desde, LocalDate hasta,
                                               TipoEvento tipo, Idioma idioma) {
        List<Evento> eventos = tipo == null
                ? eventoRepository.findEnRango(EstadoEvento.PUBLICADO, desde, hasta)
                : eventoRepository.findEnRangoPorTipo(EstadoEvento.PUBLICADO, tipo, desde, hasta);

        return eventos.stream().map(evento -> mapper.aResumen(evento, idioma)).toList();
    }

    /**
     * El mes completo, de dia 1 al ultimo (RF-79).
     *
     * <p>El mes se construye con {@link YearMonth} y no restando dias: asi
     * febrero de un bisiesto tiene sus 29 sin ningun caso especial.</p>
     */
    @Transactional(readOnly = true)
    public List<EventoResumenResponse> delMes(int anio, int mes, TipoEvento tipo, Idioma idioma) {
        YearMonth periodo = YearMonth.of(anio, mes);
        return enRango(periodo.atDay(1), periodo.atEndOfMonth(), tipo, idioma);
    }

    /**
     * Proximos eventos para la portada, con su cuenta regresiva (RF-84, RF-85).
     *
     * <p>El "hoy" se calcula <strong>en la zona de Ayacucho</strong>. La JVM
     * corre en UTC, asi que a partir de las siete de la tarde de Huamanga
     * {@code LocalDate.now()} ya seria el dia siguiente y una fiesta que se
     * celebra hoy desapareceria de la portada esta misma tarde.</p>
     */
    @Transactional(readOnly = true)
    public List<EventoResumenResponse> proximos(int limite, TipoEvento tipo, Idioma idioma) {
        LocalDate hoy = TiempoAyacucho.hoy(clock);
        var pagina = PageRequest.of(0, Math.clamp(limite, 1, 20));

        List<Evento> eventos = tipo == null
                ? eventoRepository.findProximos(EstadoEvento.PUBLICADO, hoy, pagina)
                : eventoRepository.findProximosPorTipo(EstadoEvento.PUBLICADO, tipo, hoy, pagina);

        return eventos.stream().map(evento -> mapper.aResumen(evento, idioma)).toList();
    }

    /**
     * Ficha completa con su clima (RF-80, RF-88).
     *
     * <p>Un evento en borrador devuelve 404 y no 403, por el mismo motivo que un
     * lugar sin publicar: decir "existe pero no puedes verlo" ya confirma que
     * existe.</p>
     */
    @Transactional(readOnly = true)
    public EventoDetalleResponse detalle(UUID id, Idioma idioma) {
        Evento evento = eventoRepository.findByIdConTraducciones(id)
                .filter(candidato -> candidato.getEstado() == EstadoEvento.PUBLICADO || esAdministrador())
                .orElseThrow(() -> new RecursoNoEncontradoException("evento", id.toString()));

        return mapper.aDetalle(evento, idioma,
                climaEventos.paraEvento(evento.getFechaInicio(), evento.getFechaFin()));
    }

    /**
     * Cruce de las fechas de un viaje con la agenda (RF-84b).
     *
     * <p>Devuelve los dias del viaje uno a uno —cada cual con el clima que se
     * pueda decir de el— y los eventos que se solapan con el rango. El clima de
     * los primeros dias sera pronostico real y el de los ultimos, la temporada;
     * esa transicion la resuelve {@link ClimaEventoService} y aqui no se
     * distingue.</p>
     */
    @Transactional(readOnly = true)
    public VisitaResponse duranteMiVisita(LocalDate desde, LocalDate hasta, Idioma idioma) {
        if (hasta.isBefore(desde)) {
            throw new RangoDeViajeInvalidoException("La fecha de regreso es anterior a la de llegada");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DIAS_DE_VIAJE) {
            throw new RangoDeViajeInvalidoException(
                    "El viaje no puede pasar de " + MAXIMO_DIAS_DE_VIAJE + " dias");
        }

        List<Evento> eventos = eventoRepository.findEnRango(EstadoEvento.PUBLICADO, desde, hasta);
        List<LocalDate> dias = desde.datesUntil(hasta.plusDays(1)).toList();
        var climas = climaEventos.paraDias(dias);

        List<VisitaResponse.DiaDeViaje> detalleDias = new java.util.ArrayList<>(dias.size());
        for (int i = 0; i < dias.size(); i++) {
            LocalDate dia = dias.get(i);
            List<String> activos = eventos.stream()
                    .filter(evento -> evento.ocurreEntre(dia, dia))
                    .map(evento -> evento.getId().toString())
                    .toList();
            detalleDias.add(new VisitaResponse.DiaDeViaje(dia, climas.get(i), activos));
        }

        return new VisitaResponse(desde, hasta, detalleDias,
                eventos.stream().map(evento -> mapper.aResumen(evento, idioma)).toList());
    }

    private boolean esAdministrador() {
        var contexto = org.springframework.security.core.context.SecurityContextHolder.getContext();
        return contexto.getAuthentication() != null
                && contexto.getAuthentication().getAuthorities().stream()
                .anyMatch(autoridad -> "ROLE_ADMIN".equals(autoridad.getAuthority()));
    }

    /** Rango de viaje imposible o desmesurado. */
    public static class RangoDeViajeInvalidoException extends RuntimeException {
        public RangoDeViajeInvalidoException(String mensaje) {
            super(mensaje);
        }
    }
}
