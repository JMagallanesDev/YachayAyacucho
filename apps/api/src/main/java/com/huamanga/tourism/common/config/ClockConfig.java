package com.huamanga.tourism.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Expone el reloj del sistema como bean.
 *
 * <p>Inyectar {@link Clock} en vez de llamar a {@code Instant.now()} permite
 * fijar el tiempo en los tests. Sera imprescindible en el Bloque 3 para el
 * calculo de "abierto ahora" (RF-09b) y en el Bloque 5 para el motor de
 * recomendaciones por hora (RF-08).</p>
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
