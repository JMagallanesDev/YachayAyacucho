package com.huamanga.tourism.lugar.controller;

import com.huamanga.tourism.lugar.dto.ImagenHistoricaResponse;
import com.huamanga.tourism.lugar.service.ImagenHistoricaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Fotos historicas para el slider antes/despues (RF-11, RF-11b).
 *
 * <p>Cuelga de {@code /lugares} y no de una ruta propia porque una foto
 * historica no tiene sentido fuera de su monumento. La excepcion es el listado
 * de puntos de captura, que es global por definicion: el modo geolocalizado
 * compara tu posicion contra todos a la vez.</p>
 */
@RestController
@RequestMapping("/lugares")
@Tag(name = "Fotos historicas", description = "Comparacion antes/despues del patrimonio")
public class ImagenHistoricaController {

    private final ImagenHistoricaService imagenHistoricaService;

    public ImagenHistoricaController(ImagenHistoricaService imagenHistoricaService) {
        this.imagenHistoricaService = imagenHistoricaService;
    }

    @GetMapping("/{slug}/historia-visual")
    @Operation(summary = "Fotos historicas de un lugar",
            description = """
                    Lista vacia si el lugar no tiene ninguna, que es lo habitual:
                    solo unos pocos monumentos tienen fotografia antigua
                    localizable. Cada elemento puede venir sin urlActual, y en
                    ese caso no hay comparacion que mostrar.
                    """)
    public List<ImagenHistoricaResponse> deLugar(@PathVariable String slug) {
        return imagenHistoricaService.deLugar(slug);
    }

    @GetMapping("/historia-visual/puntos")
    @Operation(summary = "Puntos de captura para el modo «Parate aqui» (RF-11b)",
            description = "Solo las fotos que tienen punto conocido Y contraparte actual.")
    public List<ImagenHistoricaResponse> puntos() {
        return imagenHistoricaService.conPuntoDeCaptura();
    }
}
