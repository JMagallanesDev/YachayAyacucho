package com.huamanga.tourism.evento.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.evento.dto.ClonarEventoRequest;
import com.huamanga.tourism.evento.dto.EventoDetalleResponse;
import com.huamanga.tourism.evento.dto.EventoRequest;
import com.huamanga.tourism.evento.dto.EventoResumenResponse;
import com.huamanga.tourism.evento.service.AdminEventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Gestion de la agenda cultural (RF-86). Todo exige rol ADMIN, comprobado en
 * cada metodo y no confiando en que el frontend esconda un boton.
 */
@RestController
@RequestMapping("/admin/eventos")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Agenda cultural (admin)", description = "Alta, edicion y clonado anual de eventos")
public class AdminEventoController {

    private final AdminEventoService adminEventoService;

    public AdminEventoController(AdminEventoService adminEventoService) {
        this.adminEventoService = adminEventoService;
    }

    @GetMapping
    @Operation(summary = "Bandeja de eventos",
            description = "Todos los eventos, incluidos borradores, cancelados y archivados.")
    public List<EventoResumenResponse> bandeja(@RequestParam(required = false) Idioma idioma) {
        return adminEventoService.bandeja(resolverIdioma(idioma));
    }

    @PostMapping
    @Operation(summary = "Crear un evento",
            description = "Guarda el evento y sus traducciones en una sola transaccion.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o fechas incoherentes"),
            @ApiResponse(responseCode = "403", description = "Autenticado pero sin rol ADMIN")
    })
    public ResponseEntity<EventoDetalleResponse> crear(
            @Valid @RequestBody EventoRequest peticion,
            @RequestParam(required = false) Idioma idioma) {

        EventoDetalleResponse creado = adminEventoService.crear(peticion, resolverIdioma(idioma));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un evento",
            description = "Reemplaza el evento completo, incluidas sus traducciones.")
    public EventoDetalleResponse actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EventoRequest peticion,
            @RequestParam(required = false) Idioma idioma) {

        return adminEventoService.actualizar(id, peticion, resolverIdioma(idioma));
    }

    @PostMapping("/{id}/clonar")
    @Operation(summary = "Clonar al anio siguiente",
            description = """
                    Crea la edicion de otro anio copiando la plantilla (nombre,
                    descripcion, tipo, lugar y portada) pero NO la fecha vieja:
                    las festividades moviles como la Semana Santa cambian de
                    fecha cada anio. El clon nace en BORRADOR para que una
                    persona confirme las fechas antes de publicarlas.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Borrador creado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un clon de este evento para ese anio"),
            @ApiResponse(responseCode = "422", description = "El evento no esta marcado como recurrente anual")
    })
    public ResponseEntity<EventoDetalleResponse> clonar(
            @PathVariable UUID id,
            @Valid @RequestBody ClonarEventoRequest peticion,
            @RequestParam(required = false) Idioma idioma) {

        EventoDetalleResponse clon = adminEventoService.clonar(id, peticion, resolverIdioma(idioma));
        return ResponseEntity.status(HttpStatus.CREATED).body(clon);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja un evento",
            description = "Baja logica: la fila se conserva para auditoria y se puede revertir.")
    @ApiResponse(responseCode = "204", description = "Evento dado de baja")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        adminEventoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private Idioma resolverIdioma(Idioma explicito) {
        if (explicito != null) {
            return explicito;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
