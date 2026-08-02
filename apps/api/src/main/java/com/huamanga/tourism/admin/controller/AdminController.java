package com.huamanga.tourism.admin.controller;

import com.huamanga.tourism.lugar.repository.LugarRepository;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Panel de administracion. El dashboard completo llega en el Bloque 10; por
 * ahora expone un unico resumen que sirve para verificar la autorizacion por
 * rol de extremo a extremo.
 *
 * <p>{@code @PreAuthorize} en el metodo, no solo reglas por URL: si manana
 * alguien reorganiza las rutas, la restriccion viaja con el codigo que
 * protege (RNF-16).</p>
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "Administracion", description = "Operaciones reservadas al rol ADMIN")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final LugarRepository lugarRepository;

    public AdminController(UsuarioRepository usuarioRepository, LugarRepository lugarRepository) {
        this.usuarioRepository = usuarioRepository;
        this.lugarRepository = lugarRepository;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resumen de contenido (solo ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen"),
            @ApiResponse(responseCode = "401", description = "Sin autenticar"),
            @ApiResponse(responseCode = "403", description = "Autenticado pero sin rol ADMIN")
    })
    public Map<String, Long> resumen() {
        return Map.of(
                "usuarios", usuarioRepository.count(),
                "lugares", lugarRepository.count());
    }
}
