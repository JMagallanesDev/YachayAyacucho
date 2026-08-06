package com.huamanga.tourism.moderacion.controller;

import com.huamanga.tourism.common.seguridad.AntiSpam;
import com.huamanga.tourism.moderacion.dto.ReporteContenidoRequest;
import com.huamanga.tourism.moderacion.service.ReporteContenidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Reportar contenido inapropiado (RF-45).
 *
 * <p>Exige cuenta a proposito: un reporte anonimo no se puede limitar a uno por
 * persona, y sin ese limite bastaria con recargar tres veces para retirar
 * cualquier reseña incomoda.</p>
 */
@RestController
@RequestMapping("/reportes-contenido")
@Tag(name = "Reportes de contenido", description = "Denunciar fotos o resenas inapropiadas")
@SecurityRequirement(name = "bearer-jwt")
public class ReporteContenidoController {

    private final ReporteContenidoService reporteService;
    private final AntiSpam antiSpam;

    public ReporteContenidoController(ReporteContenidoService reporteService, AntiSpam antiSpam) {
        this.reporteService = reporteService;
        this.antiSpam = antiSpam;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reportar una foto o una resena",
            description = "Al tercer reporte de personas distintas, el contenido pasa a revision.")
    public ResponseEntity<Map<String, Boolean>> reportar(
            @Valid @RequestBody ReporteContenidoRequest peticion) {

        antiSpam.comprobarReporte();
        boolean enRevision = reporteService.reportar(peticion);

        // Se informa de si este reporte activo la revision, para poder decir
        // "gracias, ya esta en revision" en vez del generico "gracias".
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("enRevision", enRevision));
    }
}
