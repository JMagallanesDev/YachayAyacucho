package com.huamanga.tourism.auth;

import com.jayway.jsonpath.JsonPath;
import com.huamanga.tourism.soporte.BasePostgis;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorizacion por rol en endpoints protegidos (RNF-16).
 *
 * <p>La distincion entre 401 y 403 no es cosmetica: 401 significa "no se
 * quien eres" y 403 "se quien eres y no puedes". Si el backend devolviera 401
 * en ambos casos, el frontend mandaria al usuario a iniciar sesion cuando ya
 * la tiene, en un bucle sin salida.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Autorizacion por rol")
class AutorizacionPorRolTest extends BasePostgis {

    private static final String PASSWORD = "Yachay2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void limpiar() {
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");
        var claves = redis.keys("rate:*");
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
    }

    @Test
    @DisplayName("endpoint de admin sin autenticar devuelve 401")
    void sinAutenticarDevuelve401() throws Exception {
        mockMvc.perform(get("/admin/resumen"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("no-autenticado"));
    }

    @Test
    @DisplayName("endpoint de admin con rol USUARIO devuelve 403, no 401")
    void conRolInsuficienteDevuelve403() throws Exception {
        String token = tokenDeUsuarioNormal("basico@yachay.pe");

        mockMvc.perform(get("/admin/resumen").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("acceso-denegado"));
    }

    @Test
    @DisplayName("endpoint de admin con rol ADMIN devuelve 200")
    void conRolAdminDevuelve200() throws Exception {
        String token = tokenDeAdmin("jefe@yachay.pe");

        mockMvc.perform(get("/admin/resumen").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarios").exists());
    }

    @Test
    @DisplayName("un usuario no puede escalar privilegios cambiando el claim del token")
    void noSePuedeEscalarPrivilegios() throws Exception {
        String token = tokenDeUsuarioNormal("aspirante@yachay.pe");

        // Se reescribe el payload para poner rol ADMIN y se vuelve a montar el
        // token con la firma original.
        String[] partes = token.split("\\.");
        String cargaManipulada = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(java.util.Base64.getUrlDecoder().decode(partes[1]))
                        .replace("\"rol\":\"USUARIO\"", "\"rol\":\"ADMIN\"")
                        .getBytes());
        String falsificado = partes[0] + "." + cargaManipulada + "." + partes[2];

        // La firma cubre el payload: cualquier cambio la invalida.
        mockMvc.perform(get("/admin/resumen").header("Authorization", "Bearer " + falsificado))
                .andExpect(status().isUnauthorized());
    }

    private String tokenDeUsuarioNormal(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","nombre":"Usuario"}
                        """.formatted(email, PASSWORD)));
        return iniciarSesion(email);
    }

    private String tokenDeAdmin(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","nombre":"Admin"}
                        """.formatted(email, PASSWORD)));
        jdbc.update("""
                UPDATE usuario SET rol_id = (SELECT id FROM rol WHERE nombre = 'ADMIN')
                WHERE email = ?
                """, email);
        return iniciarSesion(email);
    }

    private String iniciarSesion(String email) throws Exception {
        String cuerpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.accessToken");
    }
}
