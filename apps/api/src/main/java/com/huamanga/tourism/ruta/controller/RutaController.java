package com.huamanga.tourism.ruta.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.ruta.dto.RutaResponse;
import com.huamanga.tourism.ruta.service.RutaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Rutas tematicas para el mapa (RF-20). Publicas y de solo lectura. */
@RestController
@RequestMapping("/rutas")
@Tag(name = "Rutas", description = "Rutas tematicas del patrimonio")
public class RutaController {

    private final RutaService rutaService;

    public RutaController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @GetMapping
    @Operation(summary = "Listar rutas activas con su recorrido ordenado")
    public List<RutaResponse> listar(@RequestParam(required = false) Idioma idioma) {
        return rutaService.listarActivas(resolver(idioma));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Una ruta por su slug")
    public RutaResponse porSlug(@PathVariable String slug,
                                @RequestParam(required = false) Idioma idioma) {
        return rutaService.buscarPorSlug(slug, resolver(idioma));
    }

    private Idioma resolver(Idioma idioma) {
        if (idioma != null) {
            return idioma;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
