package com.huamanga.tourism.auth;

import com.jayway.jsonpath.JsonPath;
import com.huamanga.tourism.auth.config.PropiedadesSeguridad;
import com.huamanga.tourism.soporte.BasePostgis;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seguridad del registro, el login y la validacion de tokens.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Seguridad de autenticacion")
class AutenticacionSeguridadTest extends BasePostgis {

    private static final String PASSWORD_VALIDA = "Yachay2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PropiedadesSeguridad propiedades;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void limpiar() {
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");
        // El rate limiting cuenta por IP en Redis y todos los tests salen de
        // la misma: sin reiniciar los contadores, el limite de 10 intentos
        // por minuto en /auth tumbaria la suite a mitad de camino.
        var claves = redis.keys("rate:*");
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
    }

    // ---------------------------------------------------------------
    //  Registro
    // ---------------------------------------------------------------

    @Test
    @DisplayName("registra un usuario y guarda la contrasena con BCrypt coste 12 (RNF-12)")
    void registraConBcryptCoste12() throws Exception {
        registrar("nuevo@yachay.pe", PASSWORD_VALIDA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nuevo@yachay.pe"))
                .andExpect(jsonPath("$.rol").value("USUARIO"));

        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM usuario WHERE email = 'nuevo@yachay.pe'", String.class);

        // El prefijo declara algoritmo y coste: $2a$ = BCrypt, 12 = coste.
        assertThat(hash).startsWith("$2a$12$");
        assertThat(hash).isNotEqualTo(PASSWORD_VALIDA);
    }

    @Test
    @DisplayName("la contrasena nunca aparece en la respuesta")
    void nuncaDevuelveLaContrasena() throws Exception {
        String cuerpo = registrar("discreto@yachay.pe", PASSWORD_VALIDA)
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain(PASSWORD_VALIDA);
        assertThat(cuerpo).doesNotContain("password");
        assertThat(cuerpo).doesNotContain("$2a$");
    }

    @Test
    @DisplayName("rechaza contrasenas debiles con 400 y detalle por campo")
    void rechazaContrasenaDebil() throws Exception {
        registrar("debil@yachay.pe", "12345678")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").exists());

        registrar("corta@yachay.pe", "Ab1")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rechaza un correo ya registrado con 409")
    void rechazaEmailDuplicado() throws Exception {
        registrar("repetido@yachay.pe", PASSWORD_VALIDA).andExpect(status().isCreated());
        registrar("repetido@yachay.pe", PASSWORD_VALIDA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("email-registrado"));
    }

    // ---------------------------------------------------------------
    //  Login
    // ---------------------------------------------------------------

    @Test
    @DisplayName("el login devuelve access token y deja la cookie de refresh protegida")
    void loginDevuelveTokenYCookieSegura() throws Exception {
        registrar("acceso@yachay.pe", PASSWORD_VALIDA);

        MvcResult resultado = login("acceso@yachay.pe", PASSWORD_VALIDA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiraEnSegundos").value(900))
                .andExpect(jsonPath("$.usuario.email").value("acceso@yachay.pe"))
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        String cookie = cabeceraDeLaCookieDeSesion(resultado);

        // Se afirma sobre la cabecera real, no sobre la intencion del codigo.
        assertThat(cookie).contains(PropiedadesSeguridad.COOKIE_REFRESH);
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("SameSite=" + propiedades.cookieSameSite());
        // Path=/ y no /api/v1/auth: con la ruta restringida, el proxy de
        // Next.js no veia la cookie y rebotaba al usuario a /login justo
        // despues de un login correcto.
        assertThat(cookie).contains("Path=/");
    }

    @Test
    @DisplayName("el login borra la cookie que quedara guardada con la ruta antigua")
    void loginLimpiaLaCookieHeredada() throws Exception {
        registrar("herencia@yachay.pe", PASSWORD_VALIDA);

        List<String> cookies = login("herencia@yachay.pe", PASSWORD_VALIDA)
                .andReturn().getResponse().getHeaders("Set-Cookie");

        // Un navegador que ya tuviera la cookie con la ruta antigua acabaria
        // con dos del mismo nombre, y enviaria primero la caducada por ser la
        // de ruta mas especifica: la sesion se rompia sola.
        assertThat(cookies).hasSize(2);
        assertThat(cookies).anyMatch(c -> c.contains("Path=/api/v1/auth") && c.contains("Max-Age=0"));
        assertThat(cookies).anyMatch(c -> c.contains("Path=/") && !c.contains("Max-Age=0"));
    }

    /** La cabecera Set-Cookie que trae la sesion nueva, no la de borrado. */
    private String cabeceraDeLaCookieDeSesion(MvcResult resultado) {
        return resultado.getResponse().getHeaders("Set-Cookie").stream()
                .filter(c -> !c.contains("Max-Age=0"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se emitio ninguna cookie de sesion"));
    }

    @Test
    @DisplayName("el access token caduca en 15 minutos, no en 24 horas")
    void accessTokenDeVidaCorta() {
        // Un JWT no se puede revocar: su unica defensa es durar poco.
        assertThat(propiedades.accessExpiration().toMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("el refresh token NO se guarda en claro en la base de datos")
    void refreshTokenSeGuardaHasheado() throws Exception {
        registrar("hash@yachay.pe", PASSWORD_VALIDA);
        MvcResult resultado = login("hash@yachay.pe", PASSWORD_VALIDA).andReturn();

        String valorCookie = extraerValorCookie(resultado);
        String guardado = jdbc.queryForObject(
                "SELECT token_hash FROM refresh_token", String.class);

        // Si alguien lee la tabla, no obtiene nada reutilizable.
        assertThat(guardado).isNotEqualTo(valorCookie);
        assertThat(guardado).hasSize(64);   // SHA-256 en hexadecimal
    }

    // ---------------------------------------------------------------
    //  Enumeracion de usuarios
    // ---------------------------------------------------------------

    @Test
    @DisplayName("no revela si un correo existe: mismo codigo y mismo cuerpo en ambos casos")
    void noPermiteEnumerarUsuarios() throws Exception {
        registrar("existe@yachay.pe", PASSWORD_VALIDA);

        MvcResult passwordMala = login("existe@yachay.pe", "OtraClave123").andReturn();
        MvcResult correoInexistente = login("noexiste@yachay.pe", "OtraClave123").andReturn();

        assertThat(passwordMala.getResponse().getStatus()).isEqualTo(401);
        assertThat(correoInexistente.getResponse().getStatus()).isEqualTo(401);

        // Identicos salvo el instante de la respuesta, que solo refleja
        // cuando ocurrio y no distingue un caso del otro. Nada mas en el
        // cuerpo permite averiguar si el correo existe.
        assertThat(sinMarcaDeTiempo(correoInexistente))
                .isEqualTo(sinMarcaDeTiempo(passwordMala));
    }

    private String sinMarcaDeTiempo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString()
                .replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":\"<omitido>\"");
    }

    // ---------------------------------------------------------------
    //  Validacion del access token
    // ---------------------------------------------------------------

    @Test
    @DisplayName("sin cabecera Authorization devuelve 401")
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("no-autenticado"));
    }

    @Test
    @DisplayName("con un token valido devuelve los datos del usuario")
    void conTokenValidoDevuelveDatos() throws Exception {
        registrar("valido@yachay.pe", PASSWORD_VALIDA);
        String token = tokenDe(login("valido@yachay.pe", PASSWORD_VALIDA).andReturn());

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("valido@yachay.pe"));
    }

    @Test
    @DisplayName("un token con la firma manipulada devuelve 401")
    void tokenManipuladoDevuelve401() throws Exception {
        registrar("manipulado@yachay.pe", PASSWORD_VALIDA);
        String token = tokenDe(login("manipulado@yachay.pe", PASSWORD_VALIDA).andReturn());

        // Se altera el ultimo caracter de la firma.
        String alterado = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token firmado con otro secreto devuelve 401")
    void tokenDeOtroEmisorDevuelve401() throws Exception {
        String secretoAjeno = "secreto-de-otro-sistema-completamente-distinto-1234";
        JwtEncoder encoderAjeno = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secretoAjeno.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

        String token = encoderAjeno.encode(JwtEncoderParameters.from(
                JwsHeader.with(() -> "HS256").build(),
                JwtClaimsSet.builder()
                        .issuer(propiedades.emisor())
                        .subject(UUID.randomUUID().toString())
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                        .claim("rol", "ADMIN")
                        .build())).getTokenValue();

        // Aunque el contenido diga ADMIN, la firma no valida con nuestra clave.
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token caducado devuelve 401")
    void tokenCaducadoDevuelve401() throws Exception {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(propiedades.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(() -> "HS256").build(),
                JwtClaimsSet.builder()
                        .issuer(propiedades.emisor())
                        .subject(UUID.randomUUID().toString())
                        .issuedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                        .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                        .claim("rol", "USUARIO")
                        .build())).getTokenValue();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token con emisor distinto devuelve 401")
    void tokenConEmisorDistintoDevuelve401() throws Exception {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(propiedades.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(() -> "HS256").build(),
                JwtClaimsSet.builder()
                        .issuer("otro-sistema")
                        .subject(UUID.randomUUID().toString())
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                        .claim("rol", "USUARIO")
                        .build())).getTokenValue();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    //  Utilidades
    // ---------------------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions registrar(String email, String password)
            throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","nombre":"Persona de prueba"}
                        """.formatted(email, password)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password)
            throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }

    private String tokenDe(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");
    }

    /** La cookie de sesion, descartando la cabecera de borrado de la heredada. */
    private String extraerValorCookie(MvcResult resultado) {
        return java.util.Arrays.stream(resultado.getResponse().getCookies())
                .filter(c -> PropiedadesSeguridad.COOKIE_REFRESH.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se emitio ninguna cookie de sesion"));
    }
}
