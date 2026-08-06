package com.huamanga.tourism.foto.controller;

import com.huamanga.tourism.common.seguridad.AntiSpam;
import com.huamanga.tourism.foto.dto.FotoResponse;
import com.huamanga.tourism.foto.service.FotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Fotos de un lugar (RF-38).
 *
 * <p>La galeria publica solo devuelve las aprobadas. Las pendientes las ve
 * unicamente quien las subio, para que sepa que su foto esta en cola y no
 * vuelva a subirla pensando que se perdio.</p>
 */
@RestController
@RequestMapping("/lugares/{slug}/fotos")
@Tag(name = "Fotos", description = "Galeria de fotos subidas por los visitantes")
public class FotoController {

    private final FotoService fotoService;
    private final AntiSpam antiSpam;

    public FotoController(FotoService fotoService, AntiSpam antiSpam) {
        this.fotoService = fotoService;
        this.antiSpam = antiSpam;
    }

    @GetMapping
    @Operation(summary = "Fotos aprobadas de un lugar")
    public List<FotoResponse> galeria(@PathVariable String slug) {
        return fotoService.aprobadasDe(slug);
    }

    @GetMapping("/mias")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Las fotos del usuario actual, incluidas las pendientes")
    public List<FotoResponse> mias(@PathVariable String slug) {
        return fotoService.miasEn(slug);
    }

    /**
     * Sube una foto.
     *
     * <p>Se recibe como multipart y <strong>se valida antes de subir nada</strong>
     * a Cloudinary: firma de bytes, decodificacion real y tamaño (RNF-15). La
     * cabecera {@code Content-Type} que envie el cliente se ignora por completo.</p>
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Subir una foto; queda pendiente de moderacion")
    public ResponseEntity<FotoResponse> subir(@PathVariable String slug,
                                              @RequestParam("archivo") MultipartFile archivo) {
        antiSpam.comprobarFoto();
        FotoResponse subida = fotoService.subir(slug, archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(subida);
    }
}
