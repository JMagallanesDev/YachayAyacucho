package com.huamanga.tourism.checkin.controller;

import com.huamanga.tourism.checkin.dto.CheckInRequest;
import com.huamanga.tourism.checkin.dto.CheckInResponse;
import com.huamanga.tourism.checkin.service.CheckInService;
import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.seguridad.AntiSpam;
import com.huamanga.tourism.favorito.service.FavoritoService;
import com.huamanga.tourism.insignia.dto.PasaporteResponse;
import com.huamanga.tourism.insignia.service.PasaporteService;
import com.huamanga.tourism.lugar.dto.LugarResumenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Participacion del visitante: favoritos, visitas y pasaporte
 * (RF-35, RF-39, RF-39b).
 *
 * <p>Todo aqui exige cuenta y opera <strong>sobre el usuario autenticado</strong>:
 * ningun endpoint recibe un identificador de usuario. Es deliberado. Si la ruta
 * fuera {@code /usuarios/{id}/favoritos}, bastaria con olvidar una comprobacion
 * para que cualquiera leyera los favoritos ajenos; tomando siempre el id del
 * token, ese error no se puede cometer.</p>
 */
@RestController
@RequestMapping
@Tag(name = "Participacion", description = "Favoritos, check-in y pasaporte patrimonial")
@SecurityRequirement(name = "bearer-jwt")
public class ParticipacionController {

    private final FavoritoService favoritoService;
    private final CheckInService checkInService;
    private final PasaporteService pasaporteService;
    private final AntiSpam antiSpam;

    public ParticipacionController(FavoritoService favoritoService,
                                   CheckInService checkInService,
                                   PasaporteService pasaporteService,
                                   AntiSpam antiSpam) {
        this.favoritoService = favoritoService;
        this.checkInService = checkInService;
        this.pasaporteService = pasaporteService;
        this.antiSpam = antiSpam;
    }

    // ---------------------------------------------------------------
    //  Favoritos (RF-35)
    // ---------------------------------------------------------------

    @PostMapping("/lugares/{slug}/favorito")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar o desmarcar un lugar como favorito")
    public Map<String, Boolean> alternarFavorito(@PathVariable String slug) {
        return Map.of("favorito", favoritoService.alternar(slug));
    }

    @GetMapping("/lugares/{slug}/favorito")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Saber si el lugar esta entre mis favoritos")
    public Map<String, Boolean> esFavorito(@PathVariable String slug) {
        return Map.of("favorito", favoritoService.esFavorito(slug));
    }

    @DeleteMapping("/lugares/{slug}/favorito")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Quitar de favoritos")
    public ResponseEntity<Void> quitarFavorito(@PathVariable String slug) {
        if (favoritoService.esFavorito(slug)) {
            favoritoService.alternar(slug);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/perfil/favoritos")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mis lugares guardados")
    public Page<LugarResumenResponse> misFavoritos(
            @RequestParam(required = false) Idioma idioma,
            @PageableDefault(size = 20) Pageable pagina) {
        return favoritoService.mios(resolver(idioma), pagina);
    }

    // ---------------------------------------------------------------
    //  Check-in (RF-39)
    // ---------------------------------------------------------------

    @PostMapping("/lugares/{slug}/check-in")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar una visita estando cerca del lugar",
            description = """
                    La proximidad se comprueba en el servidor con PostGIS sobre
                    `geography`. Es un incentivo ludico —sellos e insignias— y
                    no una credencial: la posicion la envia el navegador y por
                    tanto es falsificable. No desbloquea nada critico.
                    """)
    public ResponseEntity<CheckInResponse> checkIn(@PathVariable String slug,
                                                   @Valid @RequestBody CheckInRequest peticion) {
        antiSpam.comprobarCheckIn();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkInService.registrar(slug, peticion));
    }

    // ---------------------------------------------------------------
    //  Pasaporte (RF-39b)
    // ---------------------------------------------------------------

    @GetMapping("/perfil/pasaporte")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sellos, insignias y progreso por ruta")
    public PasaporteResponse pasaporte(@RequestParam(required = false) Idioma idioma) {
        return pasaporteService.mio(resolver(idioma));
    }

    private Idioma resolver(Idioma idioma) {
        if (idioma != null) {
            return idioma;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
