package com.huamanga.tourism.reporte.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.reporte.domain.EstadoReporte;
import com.huamanga.tourism.reporte.dto.ReporteResponse;
import com.huamanga.tourism.reporte.service.ModeracionReportesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Bandeja de moderacion de reportes ciudadanos (RF-76).
 *
 * <p>Solo ADMIN, comprobado metodo a metodo. Es la unica puerta por la que un
 * reporte llega al mapa publico.</p>
 */
@RestController
@RequestMapping("/admin/reportes")
@Tag(name = "Moderacion de reportes", description = "Revision de denuncias ciudadanas")
@SecurityRequirement(name = "bearer-jwt")
public class ModeracionReportesController {

    private final ModeracionReportesService moderacionService;

    public ModeracionReportesController(ModeracionReportesService moderacionService) {
        this.moderacionService = moderacionService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Todos los reportes, con sus notas internas")
    public List<ReporteResponse> bandeja(@RequestParam(required = false) Idioma idioma) {
        Idioma resuelto = idioma != null ? idioma
                : ("en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES);
        return moderacionService.bandeja(resuelto);
    }

    @PostMapping("/{reporteId}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar el estado y anotar la decision",
            description = """
                    Al pasar a APROBADO o RESUELTO, el incidente entra en el
                    mapa publico y, si el reporte no era anonimo, se evalua la
                    insignia GUARDIAN de quien lo envio.
                    """)
    public ResponseEntity<Void> cambiarEstado(@PathVariable UUID reporteId,
                                              @Valid @RequestBody CambioEstado cambio) {
        moderacionService.cambiarEstado(reporteId, cambio.estado(), cambio.notas());
        return ResponseEntity.noContent().build();
    }

    /**
     * @param notas anotacion interna del moderador. No sale nunca al publico:
     *              sirve para dejar por que se tomo la decision, y a menudo
     *              contiene informacion que no debe difundirse
     */
    public record CambioEstado(
            @NotNull(message = "{reporte.estado.obligatorio}") EstadoReporte estado,
            @Size(max = 2000, message = "{reporte.notas.largas}") String notas) {
    }
}
