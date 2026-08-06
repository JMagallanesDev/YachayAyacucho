package com.huamanga.tourism.clima.service;

import com.huamanga.tourism.clima.config.CircuitBreakerConfig;
import com.huamanga.tourism.clima.config.PropiedadesClima;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Optional;

/**
 * Unica puerta hacia OpenWeatherMap.
 *
 * <p>Devuelve {@link Optional} en vez de lanzar: para quien llama, «el
 * proveedor no contesta» no es una condicion excepcional sino un caso normal
 * que hay que tratar, y modelarlo como excepcion invita a olvidarlo.</p>
 *
 * <p>La clave de API se anade aqui y solo aqui. No aparece en ningun DTO ni
 * viaja al navegador.</p>
 */
@Component
public class ClienteOpenWeather {

    private static final Logger log = LoggerFactory.getLogger(ClienteOpenWeather.class);

    private final RestClient http;
    private final PropiedadesClima propiedades;
    private final CircuitBreaker cortacircuitos;

    public ClienteOpenWeather(PropiedadesClima propiedades, CircuitBreakerRegistry registro) {
        this.propiedades = propiedades;
        this.cortacircuitos = registro.circuitBreaker(CircuitBreakerConfig.CLIMA);

        // El timeout es parte de la resiliencia, no un detalle: sin el, una
        // caida lenta del proveedor retiene hilos hasta agotar el servidor.
        //
        // Se construye con el cliente HTTP de la JDK y no con el constructor de
        // fabricas de Spring Boot: ese ayudante cambio de sitio en Boot 4,
        // mientras que esta API de Spring Framework es estable.
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(propiedades.timeout()).build());
        fabrica.setReadTimeout(propiedades.timeout());

        this.http = RestClient.builder()
                .baseUrl(propiedades.urlBase())
                .requestFactory(fabrica)
                .build();
    }

    /** Clima actual crudo, tal como lo entrega el proveedor. */
    public Optional<RespuestaClimaActual> climaActual() {
        return llamar("/data/2.5/weather", RespuestaClimaActual.class);
    }

    /** Cinco dias en pasos de tres horas: es lo que da el plan gratuito. */
    public Optional<RespuestaPronostico> pronostico() {
        return llamar("/data/2.5/forecast", RespuestaPronostico.class);
    }

    private <T> Optional<T> llamar(String ruta, Class<T> tipo) {
        if (!propiedades.configurado()) {
            // Sin clave no se intenta siquiera. Que el proyecto arranque sin
            // cuenta de OpenWeatherMap es deliberado.
            return Optional.empty();
        }

        try {
            T respuesta = cortacircuitos.executeCallable(() -> http.get()
                    .uri(constructor -> constructor
                            .path(ruta)
                            .queryParam("lat", propiedades.latitud())
                            .queryParam("lon", propiedades.longitud())
                            .queryParam("units", "metric")
                            .queryParam("appid", propiedades.apiKey())
                            .build())
                    .retrieve()
                    .body(tipo));

            return Optional.ofNullable(respuesta);

        } catch (CallNotPermittedException e) {
            // El cortacircuitos esta abierto: ni se intento. Es informacion,
            // no un error, y por eso no ensucia el log con una traza.
            log.debug("Cortacircuitos abierto para OpenWeatherMap; no se llamo a {}", ruta);
            return Optional.empty();

        } catch (Exception e) {
            // Nunca se propaga: el clima es informacion accesoria y su caida
            // no puede tumbar la pagina de un lugar. El mensaje va sin la
            // URL completa, que lleva la clave de API dentro.
            log.warn("OpenWeatherMap no respondio en {}: {}", ruta, e.getMessage());
            return Optional.empty();
        }
    }

    // ---------------------------------------------------------------
    //  Contrato del proveedor. Solo se declara lo que se usa: cualquier
    //  campo nuevo que anadan se ignora sin romper nada.
    // ---------------------------------------------------------------

    public record RespuestaClimaActual(Main main, Wind wind, java.util.List<Weather> weather, Long dt) {

        public record Main(Double temp, Double feels_like, Integer humidity) {
        }

        public record Wind(Double speed) {
        }

        public record Weather(String main, String icon, String description) {
        }
    }

    public record RespuestaPronostico(java.util.List<Paso> list) {

        public record Paso(Long dt,
                           RespuestaClimaActual.Main main,
                           java.util.List<RespuestaClimaActual.Weather> weather,
                           Double pop) {
        }
    }
}
