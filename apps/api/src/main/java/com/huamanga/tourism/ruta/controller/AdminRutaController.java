package com.huamanga.tourism.ruta.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.ruta.dto.RutaRequest;
import com.huamanga.tourism.ruta.dto.RutaResponse;
import com.huamanga.tourism.ruta.service.AdminRutaService;
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
 * CRUD de rutas tematicas (RF-53). Todo exige rol ADMIN.
 */
@RestController
@RequestMapping("/admin/rutas")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Rutas (admin)", description = "Alta, edicion y baja de rutas tematicas")
public class AdminRutaController {

    private final AdminRutaService adminRutaService;

    public AdminRutaController(AdminRutaService adminRutaService) {
        this.adminRutaService = adminRutaService;
    }

    @GetMapping
    @Operation(summary = "Bandeja de rutas",
            description = "Incluye las desactivadas, para poder reactivarlas.")
    public List<RutaResponse> bandeja(@RequestParam(required = false) Idioma idioma) {
        return adminRutaService.bandeja(resolverIdioma(idioma));
    }

    @PostMapping
    @Operation(summary = "Crear una ruta",
            description = """
                    Las paradas llegan como una lista ordenada de identificadores
                    de lugar: la posicion define el orden del recorrido, asi que
                    no puede haber dos con el mismo numero ni huecos.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ruta creada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o una parada repetida"),
            @ApiResponse(responseCode = "409", description = "Ese identificador de URL ya esta en uso")
    })
    public ResponseEntity<RutaResponse> crear(@Valid @RequestBody RutaRequest peticion,
                                              @RequestParam(required = false) Idioma idioma) {
        RutaResponse creada = adminRutaService.crear(peticion, resolverIdioma(idioma));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una ruta",
            description = "Reemplaza la ruta completa, incluidas traducciones y recorrido.")
    public RutaResponse actualizar(@PathVariable UUID id,
                                   @Valid @RequestBody RutaRequest peticion,
                                   @RequestParam(required = false) Idioma idioma) {
        return adminRutaService.actualizar(id, peticion, resolverIdioma(idioma));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja una ruta",
            description = "Baja logica: desaparece del mapa pero la fila se conserva.")
    @ApiResponse(responseCode = "204", description = "Ruta dada de baja")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        adminRutaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private Idioma resolverIdioma(Idioma explicito) {
        if (explicito != null) {
            return explicito;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
