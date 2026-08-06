package com.huamanga.tourism.reporte.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.reporte.dto.ReporteRequest;
import com.huamanga.tourism.reporte.dto.ReporteResponse;
import com.huamanga.tourism.reporte.dto.TipoIncidenteResponse;
import com.huamanga.tourism.reporte.service.AntiSpamAnonimo;
import com.huamanga.tourism.reporte.service.ReporteService;
import com.huamanga.tourism.reporte.service.TipoIncidenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Reportes ciudadanos de atentados al patrimonio (RF-69 a RF-74).
 *
 * <p><strong>Crear un reporte NO exige cuenta.</strong> Es la decision que hace
 * real el anonimato: obligar a registrarse y limitarse a ocultar el nombre
 * seria anonimato de cara a la galeria, porque el sistema seguiria sabiendo
 * quien denuncio. Quien quiera reconocimiento —y la insignia GUARDIAN— puede
 * identificarse; quien no, denuncia igual.</p>
 */
@RestController
@RequestMapping("/reportes")
@Tag(name = "Reportes ciudadanos", description = "Denuncias de danos al patrimonio")
public class ReporteController {

    private final ReporteService reporteService;
    private final TipoIncidenteService tipoService;
    private final AntiSpamAnonimo antiSpam;

    public ReporteController(ReporteService reporteService,
                             TipoIncidenteService tipoService,
                             AntiSpamAnonimo antiSpam) {
        this.reporteService = reporteService;
        this.tipoService = tipoService;
        this.antiSpam = antiSpam;
    }

    @GetMapping("/tipos")
    @Operation(summary = "Los 7 tipos de incidente, con su nombre traducido (RF-70)")
    public List<TipoIncidenteResponse> tipos(@RequestParam(required = false) Idioma idioma) {
        return tipoService.listar(resolver(idioma));
    }

    /**
     * Envia una denuncia, con hasta 5 fotos.
     *
     * <p>Va como multipart porque lleva imagenes. El limite anti-abuso se
     * comprueba <strong>antes</strong> de procesar nada, para no gastar CPU
     * validando fotos de una peticion que se va a rechazar.</p>
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Reportar un incidente; no requiere cuenta (RF-69, RF-72)")
    public ResponseEntity<ReporteResponse> crear(
            @Valid @RequestPart("reporte") ReporteRequest peticion,
            @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos,
            @RequestParam(required = false) Idioma idioma,
            HttpServletRequest solicitud) {

        antiSpam.comprobar(solicitud);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reporteService.crear(peticion, fotos, resolver(idioma)));
    }

    /**
     * Incidentes publicados dentro del area visible del mapa (RF-74).
     *
     * <p>Publico y sin cuenta: el objetivo del mapa es que cualquiera vea el
     * estado del patrimonio de su ciudad.</p>
     */
    @GetMapping("/mapa")
    @Operation(summary = "Incidentes aprobados o resueltos en un area")
    public List<ReporteResponse> mapa(
            @RequestParam double oeste,
            @RequestParam double sur,
            @RequestParam double este,
            @RequestParam double norte,
            @RequestParam(required = false) Idioma idioma) {

        return reporteService.enMapa(oeste, sur, este, norte, resolver(idioma));
    }

    @GetMapping("/mios")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mis reportes identificados; los anonimos no aparecen")
    public List<ReporteResponse> mios(@RequestParam(required = false) Idioma idioma) {
        return reporteService.mios(resolver(idioma));
    }

    private Idioma resolver(Idioma idioma) {
        if (idioma != null) {
            return idioma;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
