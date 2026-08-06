package com.huamanga.tourism.moderacion.controller;

import com.huamanga.tourism.moderacion.dto.FotoModeracionResponse;
import com.huamanga.tourism.moderacion.dto.ResenaModeracionResponse;
import com.huamanga.tourism.moderacion.service.ModeracionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Bandejas de moderacion (RF-49, RF-50).
 *
 * <p>{@code @PreAuthorize} en <strong>cada</strong> metodo, y no solo a nivel de
 * clase: si manana se anade un endpoint aqui y se olvida la anotacion, es
 * preferible que el fallo salte en la revision a que herede un permiso por
 * accidente. La regla final de SecurityConfig ya exige autenticacion, pero no
 * distingue el rol.</p>
 */
@RestController
@RequestMapping("/admin/moderacion")
@Tag(name = "Moderacion", description = "Revision de fotos y resenas (solo ADMIN)")
@SecurityRequirement(name = "bearer-jwt")
public class ModeracionController {

    private final ModeracionService moderacionService;

    public ModeracionController(ModeracionService moderacionService) {
        this.moderacionService = moderacionService;
    }

    // ---------------------------------------------------------------
    //  Fotos (RF-49)
    // ---------------------------------------------------------------

    @GetMapping("/fotos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fotos pendientes de revision, las mas antiguas primero")
    public List<FotoModeracionResponse> fotosPendientes() {
        return moderacionService.fotosPendientes();
    }

    @PostMapping("/fotos/{fotoId}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprobar una foto; pasa a la galeria publica")
    public ResponseEntity<Void> aprobarFoto(@PathVariable UUID fotoId) {
        moderacionService.aprobarFoto(fotoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/fotos/{fotoId}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rechazar una foto y borrar el binario de Cloudinary")
    public ResponseEntity<Void> rechazarFoto(@PathVariable UUID fotoId,
                                             @RequestBody(required = false) MotivoRechazo motivo) {
        moderacionService.rechazarFoto(fotoId, motivo != null ? motivo.motivo() : null);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------
    //  Reseñas (RF-50)
    // ---------------------------------------------------------------

    @GetMapping("/resenas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resenas revisables; las editadas tras publicarse van marcadas")
    public List<ResenaModeracionResponse> resenas() {
        return moderacionService.resenasParaModerar();
    }

    @PostMapping("/resenas/{resenaId}/ocultar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ocultar una resena; deja de contar en el promedio")
    public ResponseEntity<Void> ocultar(@PathVariable UUID resenaId) {
        moderacionService.ocultarResena(resenaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resenas/{resenaId}/restaurar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Devolver una resena oculta a la vista publica")
    public ResponseEntity<Void> restaurar(@PathVariable UUID resenaId) {
        moderacionService.restaurarResena(resenaId);
        return ResponseEntity.noContent().build();
    }

    /** El motivo se guarda para poder explicar la decision a quien subio la foto. */
    public record MotivoRechazo(@Size(max = 255) String motivo) {
    }
}
