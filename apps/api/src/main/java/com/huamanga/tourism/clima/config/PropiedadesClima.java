package com.huamanga.tourism.clima.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracion del modulo de clima.
 *
 * @param apiKey        clave de OpenWeatherMap. <strong>Nunca sale del
 *                      backend</strong>: el navegador habla solo con nuestro
 *                      API. Si esta vacia, el modulo responde «no disponible»
 *                      en vez de arrancar roto, para que el proyecto se pueda
 *                      levantar sin cuenta.
 * @param urlBase       raiz del API de OpenWeatherMap
 * @param latitud       Huamanga
 * @param longitud      Huamanga
 * @param ttlFresco     cuanto vale un clima antes de volver a preguntar (RF-25)
 * @param ttlUltimoBueno cuanto se guarda el ultimo dato conocido como red de
 *                      seguridad. Es deliberadamente mucho mas largo que
 *                      {@code ttlFresco}: su trabajo no es evitar llamadas,
 *                      sino sobrevivir a una caida del proveedor
 * @param timeout       corte por llamada; sin el, una caida lenta bloquea hilos
 */
@ConfigurationProperties(prefix = "clima")
public record PropiedadesClima(
        String apiKey,
        String urlBase,
        double latitud,
        double longitud,
        Duration ttlFresco,
        Duration ttlUltimoBueno,
        Duration timeout
) {

    public PropiedadesClima {
        urlBase = (urlBase == null || urlBase.isBlank()) ? "https://api.openweathermap.org" : urlBase;
        ttlFresco = ttlFresco == null ? Duration.ofMinutes(30) : ttlFresco;
        ttlUltimoBueno = ttlUltimoBueno == null ? Duration.ofHours(24) : ttlUltimoBueno;
        timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
    }

    /** Sin clave configurada no se llama al proveedor: se degrada de entrada. */
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }
}
