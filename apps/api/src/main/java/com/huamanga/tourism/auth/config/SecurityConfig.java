package com.huamanga.tourism.auth.config;

import com.huamanga.tourism.auth.service.TokenService;
import com.huamanga.tourism.common.seguridad.FiltroRateLimit;
import com.huamanga.tourism.common.seguridad.ManejadorAccesoDenegado;
import com.huamanga.tourism.common.seguridad.PuntoEntradaNoAutenticado;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Cadena de seguridad del API.
 *
 * <p>Postura por defecto: <strong>todo cerrado salvo lo que se abre
 * explicitamente</strong>. Si manana alguien anade un endpoint y olvida
 * protegerlo, queda protegido igualmente, porque la regla final es
 * {@code anyRequest().authenticated()}. La configuracion inversa —abrir todo
 * y cerrar lo sensible— falla de forma silenciosa y peligrosa.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(PropiedadesSeguridad.class)
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS_GET = {
            "/health",
            "/swagger-ui/**",
            "/swagger-ui",
            "/api-docs/**",
            "/api-docs",
            // Consultar el patrimonio no exige cuenta (RF-34). Escribir si:
            // POST, PUT y DELETE sobre /lugares caen en anyRequest() y ademas
            // llevan @PreAuthorize("hasRole('ADMIN')") en el controller.
            "/lugares",
            "/lugares/**",
            // Catalogo de categorias: alimenta los chips de filtro del listado.
            "/categorias",
            // Bloque 5. Clima, rutas y recomendaciones acompanan la visita y
            // no exigen cuenta; ninguno expone datos de usuario.
            "/clima",
            "/clima/**",
            "/rutas",
            "/rutas/**",
            "/recomendaciones",
            "/recomendaciones/**",
            // Bloque 6. Leer resenas y ver la galeria no exige cuenta (RF-34);
            // escribirlas si, y lo comprueba @PreAuthorize en cada metodo.
            // Ojo: estos patrones solo abren el GET, porque la lista
            // RUTAS_PUBLICAS_GET se aplica unicamente a ese verbo.
            "/lugares/*/resenas",
            "/lugares/*/fotos",
            // Bloque 8. El mapa de incidentes y el catalogo de tipos son
            // publicos: el objetivo del modulo es que cualquiera vea el estado
            // del patrimonio de su ciudad sin tener que registrarse.
            "/reportes/tipos",
            "/reportes/mapa",
            // Bloque 9. La agenda cultural es informacion publica de la ciudad:
            // el calendario, la ficha de un evento y el cruce con las fechas de
            // un viaje se consultan sin cuenta. Gestionarla es otra cosa y vive
            // bajo /admin/eventos con @PreAuthorize.
            "/eventos",
            "/eventos/**",
            // Catalogo geografico: alimenta los desplegables de los formularios
            // de alta. Son los 119 distritos de la region, informacion publica.
            "/distritos"
    };

    private final PropiedadesSeguridad propiedades;

    public SecurityConfig(PropiedadesSeguridad propiedades) {
        this.propiedades = propiedades;
    }

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http,
                                                 CorsConfigurationSource fuenteCors,
                                                 FiltroRateLimit filtroRateLimit,
                                                 PuntoEntradaNoAutenticado puntoEntrada,
                                                 ManejadorAccesoDenegado accesoDenegado) throws Exception {
        http
                .cors(cors -> cors.configurationSource(fuenteCors))

                // Sin CSRF de Spring: el API se autentica con un token Bearer
                // que el navegador no adjunta solo, asi que no hay nada que
                // falsificar. La excepcion es /auth/refresh, que si viaja por
                // cookie; a ese lo protegen SameSite y la cabecera
                // X-Refresh-Request que exige FiltroAntiCsrfRefresh.
                .csrf(csrf -> csrf.disable())

                // Sin sesion de servidor: ni JSESSIONID ni estado compartido.
                // Es lo que permite escalar a N instancias sin sesiones
                // pegajosas (RNF-39).
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(rutas -> rutas
                        // EL PANEL ENTERO, CERRADO POR DEFECTO (RNF-16, Bloque 10).
                        //
                        // Va lo PRIMERO a proposito: las reglas se evaluan en
                        // orden y la primera que casa decide, asi que ningun
                        // patron publico posterior puede abrir por accidente una
                        // ruta de /admin.
                        //
                        // No sustituye a los @PreAuthorize de los controladores,
                        // los respalda. Un controlador nuevo bajo /admin que
                        // olvide la anotacion queda protegido igualmente, y un
                        // dia que alguien mueva las rutas la anotacion sigue
                        // viajando con el codigo. Hay ademas un test que enumera
                        // los handlers registrados y ataca cada uno con un
                        // usuario normal, de modo que la garantia no depende de
                        // que nadie se olvide.
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, RUTAS_PUBLICAS_GET).permitAll()
                        // /logout es publico a proposito: se autentica con la
                        // cookie, no con el access token. Exigir un access
                        // token valido impediria cerrar sesion justo cuando
                        // mas falta hace, con el token ya caducado.
                        .requestMatchers(HttpMethod.POST,
                                "/auth/register", "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
                        // Denunciar un dano al patrimonio NO exige cuenta
                        // (RF-72). Es lo que hace real el anonimato: obligar a
                        // registrarse y limitarse a ocultar el nombre dejaria
                        // al sistema sabiendo igualmente quien denuncio. El
                        // abuso lo frena AntiSpamAnonimo, que cuenta por origen
                        // sin llegar a guardar la IP.
                        .requestMatchers(HttpMethod.POST, "/reportes").permitAll()
                        // Anotar una visita tampoco exige cuenta, y tiene que
                        // ser asi: casi todo el sitio se navega sin registrarse
                        // (RF-34) y son esas visitas las que interesan medir. El
                        // endpoint no acepta ningun identificador de quien
                        // visita, solo la seccion.
                        .requestMatchers(HttpMethod.POST, "/analitica/**").permitAll()
                        // Preflight de CORS: el navegador lo envia sin
                        // credenciales, no tiene sentido pedirle autenticacion.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())

                // Resource server: el filtro que extrae el Bearer, verifica la
                // firma y construye el Authentication lo pone Spring.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorAutenticacion()))
                        .authenticationEntryPoint(puntoEntrada)
                        .accessDeniedHandler(accesoDenegado))

                // 401 sin credenciales validas, 403 con credenciales pero sin
                // permisos. Ambos en formato ProblemDetail.
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(puntoEntrada)
                        .accessDeniedHandler(accesoDenegado))

                // El rate limiting va ANTES de la autenticacion: si fuera
                // despues, un atacante podria agotar CPU en verificaciones
                // BCrypt sin llegar nunca a tocar el limite.
                .addFilterBefore(filtroRateLimit, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt con coste 12 (RNF-12).
     *
     * <p>Cada incremento del coste duplica el tiempo de calculo. 12 tarda unos
     * 200 ms en hardware actual: imperceptible al iniciar sesion, y carisimo
     * para quien intente probar millones de contrasenas contra un volcado de
     * la base.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveSimetrica()));
    }

    /**
     * Verifica firma, caducidad y emisor.
     *
     * <p>Validar el {@code iss} no es adorno: impide que un token emitido por
     * otro sistema que casualmente comparta secreto sea aceptado aqui.</p>
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder
                .withSecretKey(claveSimetrica())
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(propiedades.emisor()));
        return decodificador;
    }

    /** Convierte el claim "rol" en la autoridad ROLE_x que espera hasRole(). */
    private JwtAuthenticationConverter convertidorAutenticacion() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthorityPrefix("ROLE_");
        autoridades.setAuthoritiesClaimName(TokenService.CLAIM_ROL);

        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }

    private SecretKey claveSimetrica() {
        return new SecretKeySpec(
                propiedades.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
