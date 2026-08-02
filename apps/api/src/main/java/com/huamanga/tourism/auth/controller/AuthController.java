package com.huamanga.tourism.auth.controller;

import com.huamanga.tourism.auth.config.PropiedadesSeguridad;
import com.huamanga.tourism.auth.dto.AutenticacionResponse;
import com.huamanga.tourism.auth.dto.LoginRequest;
import com.huamanga.tourism.auth.dto.RegistroRequest;
import com.huamanga.tourism.auth.dto.UsuarioResponse;
import com.huamanga.tourism.auth.exception.RefreshTokenInvalidoException;
import com.huamanga.tourism.auth.service.AuthService;
import com.huamanga.tourism.auth.service.RefreshTokenService;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Registro, inicio y cierre de sesion.
 *
 * <p>El reparto de los dos tokens es el nucleo del diseno (seccion 5.3 del
 * plan):</p>
 * <ul>
 *   <li>El <strong>access token</strong> va en el cuerpo de la respuesta y el
 *       frontend lo guarda solo en memoria. Nunca en localStorage: cualquier
 *       XSS lo leeria.</li>
 *   <li>El <strong>refresh token</strong> va en una cookie httpOnly que el
 *       JavaScript de la pagina no puede leer, precisamente porque es el que
 *       da acceso duradero.</li>
 * </ul>
 * <p>Cada uno vive donde su riesgo es menor.</p>
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacion", description = "Registro, inicio y cierre de sesion")
public class AuthController {

    /**
     * Alcance de la cookie.
     *
     * <p>Empezo siendo {@code /api/v1/auth} por minimo privilegio, pero eso la
     * hacia invisible para el resto de la aplicacion: el navegador solo envia
     * una cookie a las rutas que empiezan por su Path, de modo que el
     * {@code proxy.ts} de Next.js —que corre en {@code /perfil} y
     * {@code /admin}— nunca la veia y rebotaba al usuario a /login justo
     * despues de un login correcto.</p>
     *
     * <p>Con Path={@code /} la cookie viaja a todo el sitio, que es el
     * comportamiento habitual de una cookie de sesion. Sigue siendo httpOnly,
     * Secure y con SameSite, que son las protecciones que de verdad
     * importan.</p>
     */
    private static final String RUTA_COOKIE = "/";

    /**
     * Ruta que tuvo la cookie antes de ampliarla a {@code /}.
     *
     * <p>Un navegador que ya la guardara con la ruta antigua acaba con
     * <strong>dos</strong> cookies del mismo nombre, porque el navegador las
     * distingue por (dominio, ruta, nombre). Y al pedir
     * {@code /api/v1/auth/refresh} envia primero la de ruta mas especifica,
     * que es justo la caducada: la sesion se rompia sola sin que el usuario
     * pudiera hacer nada salvo borrar cookies a mano.</p>
     *
     * <p>Por eso el login y el logout emiten tambien una orden de borrado
     * para esa ruta. Se puede retirar cuando ya no queden navegadores con la
     * cookie antigua.</p>
     */
    private static final String RUTA_COOKIE_HEREDADA = "/api/v1/auth";

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final PropiedadesSeguridad propiedades;

    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          PropiedadesSeguridad propiedades) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.propiedades = propiedades;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar una cuenta nueva")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "409", description = "El correo ya esta registrado")
    })
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(peticion));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion",
            description = "Devuelve el access token en el cuerpo y deja el refresh token en una cookie httpOnly.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesion iniciada"),
            @ApiResponse(responseCode = "401", description = "Credenciales invalidas"),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos")
    })
    public ResponseEntity<AutenticacionResponse> iniciarSesion(@Valid @RequestBody LoginRequest peticion) {
        AuthService.SesionIniciada sesion = authService.autenticar(peticion);
        String refresh = refreshTokenService.emitir(sesion.usuario());

        return ResponseEntity.ok()
                // El borrado de la cookie heredada va primero: si el navegador
                // arrastra una de la ruta antigua, se limpia en el mismo acto
                // en que recibe la nueva.
                .header(HttpHeaders.SET_COOKIE, cookieHeredadaBorrada().toString())
                .header(HttpHeaders.SET_COOKIE, cookieRefresh(refresh).toString())
                .body(sesion.respuesta());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar el access token",
            description = "Rota el refresh token: el anterior queda invalidado en el mismo acto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalido, caducado o ya usado")
    })
    public ResponseEntity<AutenticacionResponse> renovar(
            @CookieValue(name = PropiedadesSeguridad.COOKIE_REFRESH, required = false) String refreshCookie,
            @RequestHeader(name = PropiedadesSeguridad.CABECERA_ANTI_CSRF, required = false) String cabeceraAntiCsrf) {

        exigirCabeceraAntiCsrf(cabeceraAntiCsrf);

        if (refreshCookie == null || refreshCookie.isBlank()) {
            throw new RefreshTokenInvalidoException("Peticion sin cookie de refresh");
        }

        RefreshTokenService.ResultadoRotacion rotacion = refreshTokenService.rotar(refreshCookie);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieRefresh(rotacion.tokenCrudo()).toString())
                .body(authService.construirRespuesta(rotacion.usuario()));
    }

    @GetMapping("/me")
    @Operation(summary = "Datos del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del usuario"),
            @ApiResponse(responseCode = "401", description = "Sin token valido")
    })
    public UsuarioResponse yo() {
        return authService.buscarPorId(UsuarioActual.idObligatorio());
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion",
            description = "Revoca el refresh token y borra la cookie.")
    @ApiResponse(responseCode = "204", description = "Sesion cerrada")
    public ResponseEntity<Void> cerrarSesion(
            @CookieValue(name = PropiedadesSeguridad.COOKIE_REFRESH, required = false) String refreshCookie) {

        if (refreshCookie != null && !refreshCookie.isBlank()) {
            refreshTokenService.cerrarSesion(refreshCookie);
        }

        // Se borra la cookie siempre, exista o no la sesion: cerrar sesion
        // nunca debe fallar ni dejar rastro en el navegador.
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieBorrada().toString())
                .header(HttpHeaders.SET_COOKIE, cookieHeredadaBorrada().toString())
                .build();
    }

    /**
     * Con {@code SameSite=None} la cookie viaja en peticiones cross-site y
     * vuelve a ser falsificable desde otro dominio. La defensa es exigir una
     * cabecera propia: un formulario HTML malicioso puede provocar la peticion,
     * pero no puede anadirle cabeceras sin un preflight CORS que nuestro
     * allowlist de origenes rechaza.
     *
     * <p>Con {@code SameSite=Lax} el navegador ya no envia la cookie
     * cross-site, asi que la comprobacion no hace falta.</p>
     */
    private void exigirCabeceraAntiCsrf(String valor) {
        boolean cookieCrossSite = "None".equalsIgnoreCase(propiedades.cookieSameSite());
        if (cookieCrossSite && (valor == null || valor.isBlank())) {
            throw new RefreshTokenInvalidoException(
                    "Peticion de refresh sin la cabecera " + PropiedadesSeguridad.CABECERA_ANTI_CSRF);
        }
    }

    private ResponseCookie cookieRefresh(String valor) {
        return ResponseCookie.from(PropiedadesSeguridad.COOKIE_REFRESH, valor)
                // httpOnly: inaccesible desde JavaScript, inmune a XSS.
                .httpOnly(true)
                // Secure: solo por HTTPS. En local es false porque el navegador
                // descartaria la cookie sobre http://localhost.
                .secure(propiedades.cookieSecure())
                .sameSite(propiedades.cookieSameSite())
                // Alcance minimo: no se envia al resto del API.
                .path(RUTA_COOKIE)
                .maxAge(propiedades.refreshExpiration())
                .build();
    }

    private ResponseCookie cookieBorrada() {
        return ResponseCookie.from(PropiedadesSeguridad.COOKIE_REFRESH, "")
                .httpOnly(true)
                .secure(propiedades.cookieSecure())
                .sameSite(propiedades.cookieSameSite())
                .path(RUTA_COOKIE)
                .maxAge(Duration.ZERO)
                .build();
    }

    /** Borra la cookie que quedara guardada con la ruta antigua. */
    private ResponseCookie cookieHeredadaBorrada() {
        return ResponseCookie.from(PropiedadesSeguridad.COOKIE_REFRESH, "")
                .httpOnly(true)
                .secure(propiedades.cookieSecure())
                .sameSite(propiedades.cookieSameSite())
                .path(RUTA_COOKIE_HEREDADA)
                .maxAge(Duration.ZERO)
                .build();
    }
}
