package com.huamanga.tourism.negocio.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.negocio.domain.EstadoNegocio;
import com.huamanga.tourism.negocio.dto.NegocioResponse;
import com.huamanga.tourism.negocio.service.AdminNegocioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.context.i18n.LocaleContextHolder;
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
 * Aprobacion de negocios (RF-104). Todo exige rol ADMIN, y ademas la regla
 * {@code /admin/**} de {@code SecurityConfig} lo respalda desde el Bloque 10.
 */
@RestController
@RequestMapping("/admin/negocios")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Negocios (admin)", description = "Revision y aprobacion del directorio")
public class AdminNegocioController {

    private final AdminNegocioService adminNegocioService;

    public AdminNegocioController(AdminNegocioService adminNegocioService) {
        this.adminNegocioService = adminNegocioService;
    }

    @GetMapping
    @Operation(summary = "Bandeja de negocios",
            description = "Todos los estados, con los pendientes de revision arriba.")
    public List<NegocioResponse> bandeja(@RequestParam(required = false) Idioma idioma) {
        return adminNegocioService.bandeja(resolverIdioma(idioma));
    }

    @PostMapping("/{negocioId}/estado")
    @Operation(summary = "Aprobar, rechazar o suspender un negocio",
            description = """
                    Aprobar lo publica en el directorio y concede el rol NEGOCIO
                    a su dueno. Rechazar guarda el motivo en la bitacora, que es
                    de donde lo lee despues el propio dueno en su panel.
                    """)
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    public NegocioResponse cambiarEstado(@PathVariable UUID negocioId,
                                         @Valid @RequestBody CambioDeEstado peticion,
                                         @RequestParam(required = false) Idioma idioma) {

        return adminNegocioService.cambiarEstado(
                negocioId, peticion.estado(), peticion.motivo(), resolverIdioma(idioma));
    }

    @Schema(description = "Nuevo estado y, opcionalmente, el motivo")
    public record CambioDeEstado(

            @NotNull(message = "{negocio.estado.obligatorio}")
            EstadoNegocio estado,

            @Schema(description = "Se lo mostrara su dueno; obligatorio de hecho al rechazar")
            @Size(max = 500, message = "{negocio.motivo.longitud}")
            String motivo
    ) {
    }

    private Idioma resolverIdioma(Idioma explicito) {
        if (explicito != null) {
            return explicito;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
