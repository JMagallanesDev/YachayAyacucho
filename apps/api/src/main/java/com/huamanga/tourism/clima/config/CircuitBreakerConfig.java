package com.huamanga.tourism.clima.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Cortacircuitos del proveedor de clima (RF-25).
 *
 * <p>Se construye a mano en vez de con el starter de Spring Boot: no existe
 * modulo de Resilience4j para Spring Boot 4 y el de la linea 3 tiene problemas
 * conocidos aqui. El nucleo, en cambio, es Java puro.</p>
 *
 * <p><strong>Que hace realmente el cortacircuitos.</strong> No es el que
 * devuelve el dato de respaldo —de eso se encarga la cache—, sino el que deja
 * de insistir. Sin el, con OpenWeatherMap caido cada visitante esperaria hasta
 * 3 segundos por su timeout antes de recibir el clima cacheado, y esa espera se
 * pagaria en cada carga de pagina. Con el abierto, la llamada se rechaza al
 * instante y la respuesta degradada sale sin demora.</p>
 */
@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(PropiedadesClima.class)
public class CircuitBreakerConfig {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerConfig.class);

    public static final String CLIMA = "openweather";

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry metricas) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig configuracion =
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        // Ventana por conteo y no por tiempo: el clima se pide
                        // pocas veces (una cada 30 min de cache), asi que una
                        // ventana temporal casi siempre estaria vacia.
                        .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                                .SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50f)
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build();

        CircuitBreakerRegistry registro = CircuitBreakerRegistry.of(configuracion);

        // Las metricas van a Actuator/Micrometer. Aqui se registran a mano
        // porque es justo lo que aportaba el starter que no se usa.
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registro).bindTo(metricas);

        CircuitBreaker cortacircuitos = registro.circuitBreaker(CLIMA);
        cortacircuitos.getEventPublisher().onStateTransition(evento ->
                log.warn("Cortacircuitos {}: {} -> {}",
                        CLIMA,
                        evento.getStateTransition().getFromState(),
                        evento.getStateTransition().getToState()));

        return registro;
    }
}
