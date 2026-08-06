package com.huamanga.tourism.resena.controller;

import com.huamanga.tourism.common.seguridad.AntiSpam;
import com.huamanga.tourism.resena.dto.ResenaRequest;
import com.huamanga.tourism.resena.dto.ResenaResponse;
import com.huamanga.tourism.resena.service.ResenaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Reseñas de un lugar (RF-37).
 *
 * <p>Leer es publico; escribir exige cuenta. Anidar las reseñas bajo el slug
 * del lugar deja la jerarquia explicita en la URL y evita que se pueda pedir
 * una reseña sin saber a que lugar pertenece.</p>
 */
@RestController
@RequestMapping("/lugares/{slug}/resenas")
@Tag(name = "Resenas", description = "Calificaciones y comentarios de los visitantes")
public class ResenaController {

    private final ResenaService resenaService;
    private final AntiSpam antiSpam;

    public ResenaController(ResenaService resenaService, AntiSpam antiSpam) {
        this.resenaService = resenaService;
        this.antiSpam = antiSpam;
    }

    @GetMapping
    @Operation(summary = "Reseñas publicadas de un lugar")
    public Page<ResenaResponse> listar(@PathVariable String slug,
                                       @PageableDefault(size = 10) Pageable pagina) {
        return resenaService.listar(slug, pagina);
    }

    @GetMapping("/mia")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "La reseña que dejo el usuario actual, si existe")
    public ResponseEntity<ResenaResponse> mia(@PathVariable String slug) {
        ResenaResponse mia = resenaService.mia(slug);
        // 204 y no 404: la ruta existe, simplemente aun no hay reseña. Un 404
        // haria pensar que el lugar no existe.
        return mia == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(mia);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Dejar una reseña; una sola por persona y lugar")
    public ResponseEntity<ResenaResponse> crear(@PathVariable String slug,
                                                @Valid @RequestBody ResenaRequest peticion) {
        antiSpam.comprobarResena();
        ResenaResponse creada = resenaService.crear(slug, peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{resenaId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Editar la propia reseña")
    public ResenaResponse editar(@PathVariable String slug,
                                 @PathVariable UUID resenaId,
                                 @Valid @RequestBody ResenaRequest peticion) {
        return resenaService.editar(resenaId, peticion);
    }

    @DeleteMapping("/{resenaId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Borrar la propia reseña (baja logica)")
    public ResponseEntity<Void> eliminar(@PathVariable String slug,
                                         @PathVariable UUID resenaId) {
        resenaService.eliminar(resenaId);
        return ResponseEntity.noContent().build();
    }
}
