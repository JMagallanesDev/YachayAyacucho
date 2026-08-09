package com.huamanga.tourism.auth;

import com.huamanga.tourism.auth.config.PropiedadesSeguridad;
import com.huamanga.tourism.soporte.BasePostgis;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rotacion del refresh token y deteccion de reutilizacion.
 *
 * <p>Es la parte del sistema que decide que pasa cuando una credencial de
 * larga duracion se filtra, asi que se prueba el camino feliz y sobre todo
 * los que no lo son.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Rotacion y reutilizacion del refresh token")
class RotacionRefreshTokenTest extends BasePostgis {

    private static final String EMAIL = "sesion@yachay.pe";
    private static final String PASSWORD = "Yachay2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void limpiar() throws Exception {
        jdbc.execute("DELETE FROM refresh_token");
        // La bitacora de administracion (RF-56, Bloque 10) referencia al
        // usuario, asi que hay que vaciarla antes de borrar cuentas.
        jdbc.execute("DELETE FROM registro_actividad");
        jdbc.execute("DELETE FROM usuario");
        var claves = redis.keys("rate:*");
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","nombre":"Sesion"}
                        """.formatted(EMAIL, PASSWORD)));
    }

    @Test
    @DisplayName("renueva el access token y entrega un refresh distinto")
    void rotaElRefreshToken() throws Exception {
        String primerRefresh = iniciarSesion();

        MvcResult renovacion = mockMvc.perform(refrescarCon(primerRefresh))
                .andExpect(status().isOk())
                .andReturn();

        String segundoRefresh = valorCookie(renovacion);

        assertThat(segundoRefresh).isNotBlank();
        assertThat(segundoRefresh).isNotEqualTo(primerRefresh);
    }

    @Test
    @DisplayName("el refresh anterior deja de servir en cuanto se rota")
    void elTokenViejoDejaDeServir() throws Exception {
        String primerRefresh = iniciarSesion();
        mockMvc.perform(refrescarCon(primerRefresh)).andExpect(status().isOk());

        // Este es el corazon de la rotacion: un token usado no vuelve a valer.
        mockMvc.perform(refrescarCon(primerRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("reutilizar un token ya rotado revoca TODAS las sesiones del usuario")
    void laReutilizacionRevocaTodasLasSesiones() throws Exception {
        // Dos sesiones legitimas: por ejemplo el movil y el portatil.
        String sesionMovil = iniciarSesion();
        String sesionPortatil = iniciarSesion();

        // El movil rota con normalidad.
        MvcResult rotacion = mockMvc.perform(refrescarCon(sesionMovil))
                .andExpect(status().isOk())
                .andReturn();
        String movilRenovado = valorCookie(rotacion);

        // Alguien reproduce el token viejo del movil: solo puede ser un robo,
        // porque el legitimo ya lo cambio.
        mockMvc.perform(refrescarCon(sesionMovil))
                .andExpect(status().isUnauthorized());

        // La respuesta no es solo rechazarlo: se cortan todas las sesiones,
        // incluida la del ladron y la del portatil. El usuario tendra que
        // volver a entrar, que es exactamente lo que debe pasar tras un robo.
        mockMvc.perform(refrescarCon(movilRenovado)).andExpect(status().isUnauthorized());
        mockMvc.perform(refrescarCon(sesionPortatil)).andExpect(status().isUnauthorized());

        Integer vivos = jdbc.queryForObject(
                "SELECT count(*) FROM refresh_token WHERE revocado_en IS NULL AND usado_en IS NULL",
                Integer.class);
        assertThat(vivos).isZero();
    }

    @Test
    @DisplayName("el logout invalida el refresh y borra la cookie")
    void logoutInvalidaLaSesion() throws Exception {
        String refresh = iniciarSesion();

        MvcResult salida = mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie(PropiedadesSeguridad.COOKIE_REFRESH, refresh)))
                .andExpect(status().isNoContent())
                .andReturn();

        // La cookie se borra con Max-Age=0.
        assertThat(salida.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");

        mockMvc.perform(refrescarCon(refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("sin cookie de refresh devuelve 401")
    void sinCookieDevuelve401() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un refresh inventado devuelve 401 sin decir por que")
    void refreshDesconocidoDevuelve401() throws Exception {
        mockMvc.perform(refrescarCon("token-que-nunca-existio"))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.errorCode").value("sesion-expirada"));
    }

    @Test
    @DisplayName("un refresh caducado devuelve 401")
    void refreshCaducadoDevuelve401() throws Exception {
        String refresh = iniciarSesion();
        jdbc.update("UPDATE refresh_token SET expira_en = NOW() - INTERVAL '1 day'");

        mockMvc.perform(refrescarCon(refresh)).andExpect(status().isUnauthorized());
    }

    private String iniciarSesion() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return valorCookie(resultado);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder refrescarCon(String refresh) {
        return post("/auth/refresh")
                .cookie(new Cookie(PropiedadesSeguridad.COOKIE_REFRESH, refresh))
                .header(PropiedadesSeguridad.CABECERA_ANTI_CSRF, "1");
    }

    /**
     * La cookie de sesion, no la de borrado.
     *
     * <p>La respuesta trae dos cabeceras {@code Set-Cookie} con el mismo
     * nombre: la sesion nueva y el borrado de la que quedara guardada con la
     * ruta antigua. {@code getCookie()} devuelve la primera que encuentra, que
     * puede ser la de borrado, asi que hay que quedarse con la que trae
     * valor.</p>
     */
    private String valorCookie(MvcResult resultado) {
        return java.util.Arrays.stream(resultado.getResponse().getCookies())
                .filter(c -> PropiedadesSeguridad.COOKIE_REFRESH.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse("");
    }
}
