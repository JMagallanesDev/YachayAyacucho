package com.huamanga.tourism.recomendacion.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.recomendacion.dto.RecomendacionResponse;
import com.huamanga.tourism.recomendacion.service.RecomendacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Recomendacion contextual (RF-08) y planificador por fecha (RF-29).
 */
@RestController
@RequestMapping("/recomendaciones")
@Tag(name = "Recomendaciones", description = "Que visitar segun la hora y el clima")
public class RecomendacionController {

    private final RecomendacionService recomendacionService;

    public RecomendacionController(RecomendacionService recomendacionService) {
        this.recomendacionService = recomendacionService;
    }

    @GetMapping
    @Operation(summary = "Que hacer ahora mismo, segun hora, clima y horarios")
    public List<RecomendacionResponse> ahora(@RequestParam(required = false) Idioma idioma) {
        return recomendacionService.ahora(resolver(idioma));
    }

    @GetMapping("/planificador")
    @Operation(summary = "Que visitar en una fecha concreta")
    public RecomendacionService.Planificacion planificar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Idioma idioma) {
        return recomendacionService.planificar(fecha, resolver(idioma));
    }

    private Idioma resolver(Idioma idioma) {
        if (idioma != null) {
            return idioma;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
