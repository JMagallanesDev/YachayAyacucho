package com.huamanga.tourism.clima.controller;

import com.huamanga.tourism.clima.dto.ClimaResponse;
import com.huamanga.tourism.clima.dto.PronosticoResponse;
import com.huamanga.tourism.clima.service.ClimaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Clima de Huamanga (RF-25, RF-26).
 *
 * <p>Publico: el clima acompana a la visita y no exige cuenta. La clave de
 * OpenWeatherMap se queda en el servidor; el navegador solo habla con este
 * endpoint.</p>
 */
@RestController
@RequestMapping("/clima")
@Tag(name = "Clima", description = "Clima actual y pronostico de Huamanga")
public class ClimaController {

    private final ClimaService climaService;

    public ClimaController(ClimaService climaService) {
        this.climaService = climaService;
    }

    @GetMapping
    @Operation(summary = "Clima actual, con marca de antiguedad si el proveedor no responde")
    public ResponseEntity<ClimaResponse> actual() {
        ClimaResponse clima = climaService.actual();

        // Cache-Control corto en el navegador: el dato ya se cachea 30 min en
        // Redis, asi que aqui basta con evitar la rafaga de peticiones de una
        // misma sesion. Un dato obsoleto no se cachea: conviene reintentar
        // pronto por si el proveedor ya volvio.
        Duration edad = clima.obsoleto() ? Duration.ofSeconds(30) : Duration.ofMinutes(5);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(edad).cachePublic())
                .body(clima);
    }

    @GetMapping("/pronostico")
    @Operation(summary = "Pronostico agregado por dias (5 dias con el plan gratuito)")
    public ResponseEntity<PronosticoResponse> pronostico() {
        PronosticoResponse pronostico = climaService.pronostico();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(30)).cachePublic())
                .body(pronostico);
    }
}
