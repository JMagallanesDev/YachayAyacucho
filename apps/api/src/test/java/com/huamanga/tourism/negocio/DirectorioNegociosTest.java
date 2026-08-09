package com.huamanga.tourism.negocio;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Directorio de negocios: aprobacion y propiedad
 * (RF-104, RF-105, RF-107, RF-110).
 *
 * <p>Las dos clases anidadas que importan son {@code Propiedad} —el rol NEGOCIO
 * no da acceso al negocio de otro— y {@code Aprobacion} —lo pendiente no se cuela
 * en el directorio publico—. El resto es CRUD.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Directorio de negocios")
class DirectorioNegociosTest extends BasePostgis {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private UUID categoria;
    private UUID distrito;

    private String tokenRosa;
    private String tokenJulio;
    private String tokenAdmin;

    @BeforeEach
    void preparar() throws Exception {
        limpiar("rate:*");
        limpiar("visita:*");

        jdbc.execute("DELETE FROM visita_negocio_diario");
        jdbc.execute("DELETE FROM negocio_traduccion");
        jdbc.execute("DELETE FROM negocio");
        jdbc.execute("DELETE FROM registro_actividad");
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");

        categoria = jdbc.queryForObject(
                "SELECT id FROM categoria_negocio WHERE codigo = 'RESTAURANTES'", UUID.class);
        distrito = jdbc.queryForObject(
                "SELECT id FROM distrito WHERE codigo = '050101'", UUID.class);

        tokenRosa = registrar("rosa@yachay.pe");
        tokenJulio = registrar("julio@yachay.pe");
        tokenAdmin = crearAdmin();
    }

    /**
     * Se limpia DESPUES, no solo antes.
     *
     * <p>Los contenedores de Testcontainers son singletons de toda la suite, asi
     * que lo que esta clase deje escrito se lo encuentra la siguiente. Y aqui no
     * es un detalle: {@code negocio} tiene una FK a {@code usuario}, de modo que
     * un negocio olvidado hace fallar el {@code DELETE FROM usuario} de
     * cualquier otro test con un error de integridad que no tiene nada que ver
     * con lo que ese test estaba probando. Ya paso una vez en el Bloque 7 con una
     * insignia ficticia.</p>
     */
    @AfterEach
    void recoger() {
        jdbc.execute("DELETE FROM visita_negocio_diario");
        jdbc.execute("DELETE FROM negocio_traduccion");
        jdbc.execute("DELETE FROM negocio");
    }

    // ---------------------------------------------------------------
    //  LA PRUEBA ESTRELLA: el rol NEGOCIO no abre el negocio de otro
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Propiedad del negocio")
    class Propiedad {

        /**
         * El fallo caracteristico de los roles intermedios: se comprueba que el
         * usuario tenga el rol y se olvida comprobar que el recurso sea suyo.
         */
        @Test
        @DisplayName("un dueno de negocio NO puede editar el negocio de otro, aunque fuerce el ID")
        void noPuedeEditarElNegocioDeOtro() throws Exception {
            UUID deRosa = registrarNegocio(tokenRosa, "Restaurante de Rosa");
            aprobar(deRosa);

            // Julio tambien tiene su negocio aprobado, asi que TIENE el rol
            // NEGOCIO. Es justo el caso peligroso: las credenciales son
            // legitimas y el rol es el correcto.
            UUID deJulio = registrarNegocio(tokenJulio, "Cafeteria de Julio");
            aprobar(deJulio);

            assertThat(rolDe("julio@yachay.pe"))
                    .as("Julio es dueno verificado: el rol no es el problema")
                    .isEqualTo("NEGOCIO");

            mockMvc.perform(put("/negocios/mios/" + deRosa + "?idioma=ES")
                            .header("Authorization", "Bearer " + tokenJulio)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("Secuestrado por Julio")))
                    .andExpect(status().isForbidden());

            assertThat(nombreDe(deRosa))
                    .as("el negocio de Rosa no se toco")
                    .isEqualTo("Restaurante de Rosa");
        }

        @Test
        @DisplayName("tampoco puede LEER el negocio de otro por su panel")
        void tampocoPuedeLeerlo() throws Exception {
            UUID deRosa = registrarNegocio(tokenRosa, "Restaurante de Rosa");

            mockMvc.perform(get("/negocios/mios/" + deRosa)
                            .header("Authorization", "Bearer " + tokenJulio))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("el listado /mios solo devuelve los propios")
        void miosSoloLosPropios() throws Exception {
            registrarNegocio(tokenRosa, "Restaurante de Rosa");
            registrarNegocio(tokenJulio, "Cafeteria de Julio");

            mockMvc.perform(get("/negocios/mios?idioma=ES")
                            .header("Authorization", "Bearer " + tokenRosa))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].negocio.nombre").value("Restaurante de Rosa"));
        }

        @Test
        @DisplayName("su dueno legitimo si puede editarlo")
        void elDuenoSiPuede() throws Exception {
            UUID deRosa = registrarNegocio(tokenRosa, "Restaurante de Rosa");

            mockMvc.perform(put("/negocios/mios/" + deRosa + "?idioma=ES")
                            .header("Authorization", "Bearer " + tokenRosa)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("Restaurante de Rosa renovado")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.negocio.nombre").value("Restaurante de Rosa renovado"));
        }

        @Test
        @DisplayName("sin sesion, el panel propio devuelve 401 y no 403")
        void sinSesion() throws Exception {
            mockMvc.perform(get("/negocios/mios"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ---------------------------------------------------------------
    //  Aprobacion: lo pendiente no existe para el publico
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Flujo de aprobacion")
    class Aprobacion {

        @Test
        @DisplayName("un negocio recien registrado nace PENDIENTE")
        void naceComoPendiente() throws Exception {
            UUID id = registrarNegocio(tokenRosa, "Restaurante de Rosa");
            assertThat(estadoDe(id)).isEqualTo("PENDIENTE");
        }

        @Test
        @DisplayName("un negocio PENDIENTE no aparece en el directorio publico")
        void elPendienteNoSeCuela() throws Exception {
            registrarNegocio(tokenRosa, "Restaurante de Rosa");

            mockMvc.perform(get("/negocios?idioma=ES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        @DisplayName("ni su ficha publica: devuelve 404")
        void suFichaTampoco() throws Exception {
            UUID id = registrarNegocio(tokenRosa, "Restaurante de Rosa");

            mockMvc.perform(get("/negocios/" + id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("tras aprobarlo si aparece, y su dueno recibe el rol NEGOCIO")
        void alAprobarloAparece() throws Exception {
            UUID id = registrarNegocio(tokenRosa, "Restaurante de Rosa");

            assertThat(rolDe("rosa@yachay.pe"))
                    .as("pedir el alta no puede otorgar el rol")
                    .isEqualTo("USUARIO");

            aprobar(id);

            mockMvc.perform(get("/negocios?idioma=ES"))
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].nombre").value("Restaurante de Rosa"));

            assertThat(rolDe("rosa@yachay.pe"))
                    .as("es la aprobacion la que convierte en dueno verificado")
                    .isEqualTo("NEGOCIO");
        }

        @Test
        @DisplayName("el estado NO se puede enviar en la peticion de alta")
        void noSePuedeAutopublicar() throws Exception {
            // Aunque el cliente mande el campo, el record no lo tiene y Jackson
            // lo descarta: no hay forma de nacer APROBADO.
            MvcResult resultado = mockMvc.perform(post("/negocios?idioma=ES")
                            .header("Authorization", "Bearer " + tokenRosa)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre": "Colado", "categoriaId": "%s", "distritoId": "%s",
                                     "estado": "APROBADO"}
                                    """.formatted(categoria, distrito)))
                    .andExpect(status().isCreated())
                    .andReturn();

            UUID id = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                    resultado.getResponse().getContentAsString(), "$.negocio.id"));

            assertThat(estadoDe(id)).isEqualTo("PENDIENTE");
        }

        @Test
        @DisplayName("editar lo publico de un negocio aprobado lo devuelve a revision")
        void editarLoDevuelveARevision() throws Exception {
            UUID id = registrarNegocio(tokenRosa, "Restaurante de Rosa");
            aprobar(id);

            mockMvc.perform(put("/negocios/mios/" + id + "?idioma=ES")
                            .header("Authorization", "Bearer " + tokenRosa)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("Otro nombre completamente distinto")))
                    .andExpect(status().isOk());

            assertThat(estadoDe(id))
                    .as("si no, se aprueba una cosa y se publica otra")
                    .isEqualTo("PENDIENTE");
        }

        @Test
        @DisplayName("cambiar solo el telefono NO lo devuelve a revision")
        void elTelefonoNoLoDevuelve() throws Exception {
            UUID id = registrarNegocio(tokenRosa, "Restaurante de Rosa");
            aprobar(id);

            mockMvc.perform(put("/negocios/mios/" + id + "?idioma=ES")
                            .header("Authorization", "Bearer " + tokenRosa)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre": "Restaurante de Rosa", "categoriaId": "%s",
                                     "distritoId": "%s", "telefono": "066 999 888"}
                                    """.formatted(categoria, distrito)))
                    .andExpect(status().isOk());

            assertThat(estadoDe(id))
                    .as("obligar a revision por un telefono conseguiria que nadie lo actualizara")
                    .isEqualTo("APROBADO");
        }

        @Test
        @DisplayName("el motivo del rechazo le llega a su dueno")
        void elMotivoLlegaAlDueno() throws Exception {
            UUID id = registrarNegocio(tokenRosa, "Restaurante de Rosa");

            mockMvc.perform(post("/admin/negocios/" + id + "/estado")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"estado": "RECHAZADO", "motivo": "Falta la direccion exacta"}
                                    """))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/negocios/mios/" + id + "?idioma=ES")
                            .header("Authorization", "Bearer " + tokenRosa))
                    .andExpect(jsonPath("$.motivoRechazo").value("Falta la direccion exacta"));
        }

        @Test
        @DisplayName("aprobar exige rol ADMIN")
        void aprobarSoloAdmin() throws Exception {
            UUID id = registrarNegocio(tokenRosa, "Restaurante de Rosa");

            mockMvc.perform(post("/admin/negocios/" + id + "/estado")
                            .header("Authorization", "Bearer " + tokenRosa)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\": \"APROBADO\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    //  WhatsApp y analitica
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Contacto y analitica")
    class ContactoYAnalitica {

        @Test
        @DisplayName("el numero de WhatsApp se normaliza al guardar, no al pintar")
        void normalizaElWhatsapp() throws Exception {
            MvcResult resultado = mockMvc.perform(post("/negocios?idioma=ES")
                            .header("Authorization", "Bearer " + tokenRosa)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre": "Con WhatsApp", "categoriaId": "%s",
                                     "distritoId": "%s", "whatsapp": "966 123 456"}
                                    """.formatted(categoria, distrito)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // 9 digitos peruanos -> se antepone el 51 del pais.
            assertThat(com.jayway.jsonpath.JsonPath.read(
                    resultado.getResponse().getContentAsString(), "$.negocio.whatsapp").toString())
                    .isEqualTo("51966123456");
        }

        @Test
        @DisplayName("abrir la ficha cuenta una visita, y recargar NO la cuenta otra vez")
        void laVisitaSeCuentaConThrottling() throws Exception {
            UUID id = registrarNegocio(tokenRosa, "Restaurante de Rosa");
            aprobar(id);

            mockMvc.perform(get("/negocios/" + id)).andExpect(status().isOk());
            mockMvc.perform(get("/negocios/" + id)).andExpect(status().isOk());
            mockMvc.perform(get("/negocios/" + id)).andExpect(status().isOk());

            Integer visitas = jdbc.queryForObject(
                    "SELECT total_visitas FROM visita_negocio_diario WHERE negocio_id = ?",
                    Integer.class, id);

            assertThat(visitas)
                    .as("tres cargas seguidas son una sola visita: la ventana anti-recarga")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("la tabla de analitica no guarda ningun identificador de quien visita")
        void sinIdentificadoresPersonales() {
            var columnas = jdbc.queryForList("""
                    SELECT column_name FROM information_schema.columns
                    WHERE table_name = 'visita_negocio_diario'
                    """, String.class);

            assertThat(columnas)
                    .as("coherente con el anonimato del Bloque 8")
                    .noneMatch(c -> c.equals("usuario_id") || c.contains("ip")
                            || c.contains("user_agent") || c.contains("huella"));
        }

        @Test
        @DisplayName("un clic de WhatsApp suma en su propia columna")
        void elClicDeWhatsappSuma() throws Exception {
            UUID id = registrarNegocio(tokenRosa, "Restaurante de Rosa");
            aprobar(id);

            mockMvc.perform(post("/analitica/negocios/" + id + "/WHATSAPP"))
                    .andExpect(status().isNoContent());

            Integer clics = jdbc.queryForObject(
                    "SELECT clics_whatsapp FROM visita_negocio_diario WHERE negocio_id = ?",
                    Integer.class, id);

            assertThat(clics).isEqualTo(1);
        }
    }

    // ---------------------------------------------------------------
    //  Ayudas
    // ---------------------------------------------------------------

    private String cuerpo(String nombre) {
        return """
                {"nombre": "%s", "categoriaId": "%s", "distritoId": "%s",
                 "telefono": "066 312 456", "whatsapp": "966123456",
                 "direccion": "Portal Union 123",
                 "traducciones": [{"idioma": "ES", "descripcion": "Descripcion de prueba"}]}
                """.formatted(nombre, categoria, distrito);
    }

    private UUID registrarNegocio(String token, String nombre) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/negocios?idioma=ES")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(nombre)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                resultado.getResponse().getContentAsString(), "$.negocio.id"));
    }

    private void aprobar(UUID negocioId) throws Exception {
        mockMvc.perform(post("/admin/negocios/" + negocioId + "/estado")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"APROBADO\"}"))
                .andExpect(status().isOk());
    }

    private String estadoDe(UUID negocioId) {
        return jdbc.queryForObject("SELECT estado FROM negocio WHERE id = ?", String.class, negocioId);
    }

    private String nombreDe(UUID negocioId) {
        return jdbc.queryForObject("SELECT nombre FROM negocio WHERE id = ?", String.class, negocioId);
    }

    private String rolDe(String email) {
        return jdbc.queryForObject("""
                SELECT r.nombre FROM usuario u JOIN rol r ON r.id = u.rol_id WHERE u.email = ?
                """, String.class, email);
    }

    private String registrar(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Yachay2026Dev", "nombre": "Persona"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Yachay2026Dev"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                login.getResponse().getContentAsString(), "$.accessToken");
    }

    private String crearAdmin() throws Exception {
        registrar("jefa@yachay.pe");
        jdbc.update("""
                UPDATE usuario SET rol_id = (SELECT id FROM rol WHERE nombre='ADMIN')
                WHERE email = 'jefa@yachay.pe'
                """);

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jefa@yachay.pe", "password": "Yachay2026Dev"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                login.getResponse().getContentAsString(), "$.accessToken");
    }

    private void limpiar(String patron) {
        var claves = redis.keys(patron);
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
    }
}
