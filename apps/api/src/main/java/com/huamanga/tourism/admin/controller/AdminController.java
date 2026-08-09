package com.huamanga.tourism.admin.controller;

import com.huamanga.tourism.admin.dto.ActividadResponse;
import com.huamanga.tourism.admin.dto.CambiarUsuarioRequest;
import com.huamanga.tourism.admin.dto.DashboardResponse;
import com.huamanga.tourism.admin.dto.UsuarioAdminResponse;
import com.huamanga.tourism.admin.repository.RegistroActividadRepository;
import com.huamanga.tourism.admin.service.DashboardService;
import com.huamanga.tourism.admin.service.GestionUsuariosService;
import com.huamanga.tourism.common.domain.Idioma;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Panel de administracion: metricas, usuarios y bitacora (RF-51, RF-52, RF-56).
 *
 * <p><strong>Doble cierre, a proposito.</strong> La clase entera lleva
 * {@code @PreAuthorize("hasRole('ADMIN')")} y ademas {@code SecurityConfig}
 * exige el mismo rol para todo {@code /admin/**}. No es redundancia inutil: la
 * regla por URL cubre a un controlador nuevo que olvide la anotacion, y la
 * anotacion cubre el dia que alguien reorganice las rutas. Hay ademas un test
 * que enumera los handlers registrados y ataca cada uno con un usuario normal,
 * de modo que la garantia no depende de que nadie se olvide de nada.</p>
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administracion", description = "Operaciones reservadas al rol ADMIN")
public class AdminController {

    private final DashboardService dashboardService;
    private final GestionUsuariosService gestionUsuarios;
    private final RegistroActividadRepository actividadRepository;

    public AdminController(DashboardService dashboardService,
                           GestionUsuariosService gestionUsuarios,
                           RegistroActividadRepository actividadRepository) {
        this.dashboardService = dashboardService;
        this.gestionUsuarios = gestionUsuarios;
        this.actividadRepository = actividadRepository;
    }

    // ---------------------------------------------------------------
    //  Dashboard (RF-52)
    // ---------------------------------------------------------------

    @GetMapping("/dashboard")
    @Operation(summary = "Metricas del panel",
            description = """
                    Totales, series diarias de los ultimos 30 dias, reparto por
                    categoria y seccion, y lo que espera en las tres bandejas de
                    moderacion. Todo en una llamada: son consultas de agregacion
                    sobre tablas pequenas y pedirlas por separado dejaria la
                    pantalla apareciendo a trozos.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metricas"),
            @ApiResponse(responseCode = "401", description = "Sin autenticar"),
            @ApiResponse(responseCode = "403", description = "Autenticado pero sin rol ADMIN")
    })
    public DashboardResponse dashboard(@RequestParam(required = false) Idioma idioma) {
        return dashboardService.metricas(resolverIdioma(idioma));
    }

    /** Se mantiene desde el Bloque 2: es el resumen que verifica la autorizacion. */
    @GetMapping("/resumen")
    @Operation(summary = "Resumen breve de contenido")
    public DashboardResponse.Totales resumen(@RequestParam(required = false) Idioma idioma) {
        return dashboardService.metricas(resolverIdioma(idioma)).totales();
    }

    // ---------------------------------------------------------------
    //  Usuarios y roles (RF-51)
    // ---------------------------------------------------------------

    @GetMapping("/usuarios")
    @Operation(summary = "Listado de cuentas",
            description = """
                    Nunca devuelve la contrasena ni su hash: el DTO no tiene ese
                    campo, asi que el dato no puede escaparse por descuido.
                    """)
    public List<UsuarioAdminResponse> usuarios() {
        return gestionUsuarios.listar();
    }

    @PatchMapping("/usuarios/{usuarioId}")
    @Operation(summary = "Cambiar el rol o el estado de una cuenta",
            description = """
                    Dos barreras impiden dejar el sistema inservible: nadie puede
                    cambiarse el rol a si mismo, y nunca se puede degradar ni
                    suspender al ultimo administrador activo.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta actualizada"),
            @ApiResponse(responseCode = "404", description = "No existe esa cuenta"),
            @ApiResponse(responseCode = "409", description = "Es el ultimo administrador activo"),
            @ApiResponse(responseCode = "422", description = "Intento de cambiarse el rol a uno mismo")
    })
    public UsuarioAdminResponse cambiarUsuario(@PathVariable UUID usuarioId,
                                               @Valid @RequestBody CambiarUsuarioRequest peticion) {
        return gestionUsuarios.cambiar(usuarioId, peticion);
    }

    // ---------------------------------------------------------------
    //  Registro de actividad (RF-56)
    // ---------------------------------------------------------------

    @GetMapping("/actividad")
    @Operation(summary = "Bitacora de acciones administrativas",
            description = """
                    Quien hizo que, sobre que y desde donde. Aqui la IP SI se
                    guarda: es auditoria interna de un usuario identificado
                    ejerciendo privilegios, no una denuncia ciudadana anonima.
                    """)
    @Transactional(readOnly = true)
    public List<ActividadResponse> actividad(@RequestParam(defaultValue = "50") int limite) {
        return actividadRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.clamp(limite, 1, 200)))
                .stream()
                .map(registro -> new ActividadResponse(
                        registro.getId(),
                        registro.getAccion(),
                        registro.getEntidad(),
                        registro.getEntidadId(),
                        registro.getDetalles(),
                        registro.getUsuario().getNombre(),
                        registro.getUsuario().getEmail(),
                        registro.getIp(),
                        registro.getCreatedAt()))
                .toList();
    }

    private Idioma resolverIdioma(Idioma explicito) {
        if (explicito != null) {
            return explicito;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
