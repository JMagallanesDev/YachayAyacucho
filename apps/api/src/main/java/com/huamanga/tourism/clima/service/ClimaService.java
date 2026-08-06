package com.huamanga.tourism.clima.service;

import com.huamanga.tourism.clima.dto.ClimaResponse;
import com.huamanga.tourism.clima.dto.PronosticoResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Clima de Huamanga (RF-25, RF-26, RF-27).
 *
 * <p>El servicio no sabe nada de resiliencia: la cache decide si sirve un dato
 * fresco, uno obsoleto o ninguno, y el cliente decide si llega a llamar. Aqui
 * solo se traduce la respuesta del proveedor al contrato propio.</p>
 */
@Service
public class ClimaService {

    public static final ZoneId ZONA_AYACUCHO = ZoneId.of("America/Lima");

    private static final String CLAVE_ACTUAL = "clima:actual";
    private static final String CLAVE_PRONOSTICO = "clima:pronostico";

    /** Horas en las que alguien visita de verdad, para resumir el dia. */
    private static final int HORA_VISITA_DESDE = 8;
    private static final int HORA_VISITA_HASTA = 19;

    private final ClienteOpenWeather cliente;
    private final CacheClima cache;
    private final ConsejosClima consejos;
    private final Clock clock;

    public ClimaService(ClienteOpenWeather cliente, CacheClima cache,
                        ConsejosClima consejos, Clock clock) {
        this.cliente = cliente;
        this.cache = cache;
        this.consejos = consejos;
        this.clock = clock;
    }

    public ClimaResponse actual() {
        return cache.resolver(
                CLAVE_ACTUAL,
                ClimaResponse.class,
                () -> cliente.climaActual().map(this::aClima),
                ClimaResponse::comoObsoleto,
                ClimaResponse::noDisponible);
    }

    public PronosticoResponse pronostico() {
        return cache.resolver(
                CLAVE_PRONOSTICO,
                PronosticoResponse.class,
                () -> cliente.pronostico().map(this::aPronostico),
                PronosticoResponse::comoObsoleto,
                PronosticoResponse::noDisponible);
    }

    /**
     * Pronostico de un dia concreto, para el planificador (RF-29).
     *
     * <p>Devuelve vacio si la fecha cae fuera del alcance del pronostico. Quien
     * llama decide que hacer: el planificador responde igualmente con las
     * recomendaciones que solo dependen del horario.</p>
     */
    public Optional<PronosticoResponse.PronosticoDia> paraFecha(LocalDate fecha) {
        return pronostico().dias().stream()
                .filter(dia -> dia.fecha().equals(fecha))
                .findFirst();
    }

    // ---------------------------------------------------------------
    //  Traduccion del contrato del proveedor al propio
    // ---------------------------------------------------------------

    private ClimaResponse aClima(ClienteOpenWeather.RespuestaClimaActual respuesta) {
        String condicion = primeraCondicion(respuesta.weather());
        Double temperatura = respuesta.main() != null ? respuesta.main().temp() : null;
        Double viento = respuesta.wind() != null ? respuesta.wind().speed() : null;

        LocalTime hora = LocalDateTime.ofInstant(clock.instant(), ZONA_AYACUCHO).toLocalTime();

        return new ClimaResponse(
                temperatura,
                respuesta.main() != null ? respuesta.main().feels_like() : null,
                respuesta.main() != null ? respuesta.main().humidity() : null,
                viento,
                condicion,
                primerIcono(respuesta.weather()),
                consejos.paraAhora(condicion, temperatura, viento, hora),
                respuesta.dt() != null ? Instant.ofEpochSecond(respuesta.dt()) : clock.instant(),
                true,
                false);
    }

    /**
     * Agrega los pasos de tres horas en dias.
     *
     * <p>Para la minima y la maxima se usa el dia entero, porque el frio de
     * madrugada es justo lo que hay que avisar. Para la condicion dominante, en
     * cambio, solo cuentan las <strong>horas en que se visita</strong>: que
     * llueva a las tres de la madrugada no deberia pintar el dia de lluvia.</p>
     */
    private PronosticoResponse aPronostico(ClienteOpenWeather.RespuestaPronostico respuesta) {
        Map<LocalDate, List<ClienteOpenWeather.RespuestaPronostico.Paso>> porDia = new TreeMap<>(
                respuesta.list().stream().collect(Collectors.groupingBy(paso ->
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(paso.dt()), ZONA_AYACUCHO)
                                .toLocalDate())));

        List<PronosticoResponse.PronosticoDia> dias = porDia.entrySet().stream()
                .map(entrada -> resumirDia(entrada.getKey(), entrada.getValue()))
                .toList();

        return new PronosticoResponse(dias, clock.instant(), true, false);
    }

    private PronosticoResponse.PronosticoDia resumirDia(
            LocalDate fecha, List<ClienteOpenWeather.RespuestaPronostico.Paso> pasos) {

        Double minima = pasos.stream()
                .map(p -> p.main() != null ? p.main().temp() : null)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);

        Double maxima = pasos.stream()
                .map(p -> p.main() != null ? p.main().temp() : null)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);

        List<ClienteOpenWeather.RespuestaPronostico.Paso> enHorasDeVisita = pasos.stream()
                .filter(p -> {
                    int hora = LocalDateTime.ofInstant(Instant.ofEpochSecond(p.dt()), ZONA_AYACUCHO)
                            .getHour();
                    return hora >= HORA_VISITA_DESDE && hora <= HORA_VISITA_HASTA;
                })
                .toList();

        // Si el dia empieza ya entrada la tarde (el primer dia del pronostico
        // suele venir a medias), se usa lo que haya en vez de quedarse sin dato.
        List<ClienteOpenWeather.RespuestaPronostico.Paso> muestra =
                enHorasDeVisita.isEmpty() ? pasos : enHorasDeVisita;

        String condicion = muestra.stream()
                .map(p -> primeraCondicion(p.weather()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String icono = muestra.stream()
                .filter(p -> condicion != null && condicion.equals(primeraCondicion(p.weather())))
                .map(p -> primerIcono(p.weather()))
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(null);

        // La probabilidad del dia es la mayor de sus tramos, no la media: para
        // decidir si llevar paraguas importa el peor momento.
        Double probabilidad = muestra.stream()
                .map(ClienteOpenWeather.RespuestaPronostico.Paso::pop)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);

        return new PronosticoResponse.PronosticoDia(
                fecha, minima, maxima, condicion, icono, probabilidad,
                consejos.paraDia(condicion, minima, maxima));
    }

    private String primeraCondicion(List<ClienteOpenWeather.RespuestaClimaActual.Weather> weather) {
        return weather == null || weather.isEmpty() ? null : weather.getFirst().main();
    }

    private String primerIcono(List<ClienteOpenWeather.RespuestaClimaActual.Weather> weather) {
        return weather == null || weather.isEmpty() ? null : weather.getFirst().icon();
    }
}
