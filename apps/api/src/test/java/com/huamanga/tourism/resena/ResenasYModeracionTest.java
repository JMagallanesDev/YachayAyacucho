package com.huamanga.tourism.resena;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reseñas, calificaciones y moderacion (RF-37, RF-50).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Resenas y moderacion")
class ResenasYModeracionTest extends BasePostgis {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redis;

    private String tokenAna;
    private String tokenBruno;
    private String tokenAdmin;

    @BeforeEach
    void preparar() throws Exception {
        // El rate limiting por IP y el anti-spam por cuenta viven en Redis y
        // sobreviven entre tests: sin limpiarlos, la clase entera se queda sin
        // cupo a mitad de camino y los fallos parecen de logica cuando en
        // realidad son 429 acumulados.
        limpiarClaves("rate:*");
        limpiarClaves("antispam:*");

        jdbc.execute("DELETE FROM foto");
        jdbc.execute("DELETE FROM resena");
        jdbc.execute("DELETE FROM horario_lugar");
        jdbc.execute("DELETE FROM lugar_traduccion");
        jdbc.execute("DELETE FROM lugar");
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");

        UUID categoria = jdbc.queryForObject(
                "SELECT id FROM categoria_lugar WHERE codigo='IGLESIAS'", UUID.class);
        crearLugar("catedral", categoria, "Catedral de Ayacucho");
        crearLugar("san-francisco", categoria, "Templo de San Francisco");

        tokenAna = registrarYObtenerToken("ana@yachay.pe");
        tokenBruno = registrarYObtenerToken("bruno@yachay.pe");
        tokenAdmin = crearAdminYObtenerToken();
    }

    // ---------------------------------------------------------------
    //  Crear (RF-37)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Dejar una resena")
    class Crear {

        @Test
        @DisplayName("una persona autenticada puede calificar y comentar")
        void creaResena() throws Exception {
            mockMvc.perform(post("/lugares/catedral/resenas")
                            .header("Authorization", "Bearer " + tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"calificacion": 5, "comentario": "El retablo mayor impresiona."}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.calificacion").value(5))
                    .andExpect(jsonPath("$.autor").value("Ana"))
                    .andExpect(jsonPath("$.editada").value(false))
                    // El correo del autor no puede salir: es un dato personal.
                    .andExpect(jsonPath("$.email").doesNotExist());
        }

        @Test
        @DisplayName("sin cuenta no se puede opinar")
        void exigeAutenticacion() throws Exception {
            mockMvc.perform(post("/lugares/catedral/resenas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"calificacion": 5}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("una segunda resena del mismo usuario en el mismo lugar da 409")
        void rechazaDuplicada() throws Exception {
            calificar(tokenAna, "catedral", 5, null);

            mockMvc.perform(post("/lugares/catedral/resenas")
                            .header("Authorization", "Bearer " + tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"calificacion": 1, "comentario": "Me arrepenti."}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("resena-duplicada"));

            // Lo importante: no se colo una segunda fila que inflara la nota.
            assertThat(contar("resena")).isEqualTo(1);
        }

        @Test
        @DisplayName("el mismo usuario si puede opinar en lugares distintos")
        void permiteVariosLugares() throws Exception {
            calificar(tokenAna, "catedral", 5, null);
            calificar(tokenAna, "san-francisco", 4, null);

            assertThat(contar("resena")).isEqualTo(2);
        }

        @Test
        @DisplayName("rechaza calificaciones fuera de 1-5")
        void rechazaFueraDeRango() throws Exception {
            for (String nota : new String[]{"0", "6", "-1"}) {
                mockMvc.perform(post("/lugares/catedral/resenas")
                                .header("Authorization", "Bearer " + tokenAna)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"calificacion\": " + nota + "}"))
                        .andExpect(status().isBadRequest());
            }
            assertThat(contar("resena")).isZero();
        }

        @Test
        @DisplayName("rechaza un comentario de mas de 500 caracteres")
        void rechazaComentarioLargo() throws Exception {
            String largo = "a".repeat(501);

            mockMvc.perform(post("/lugares/catedral/resenas")
                            .header("Authorization", "Bearer " + tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"calificacion\": 4, \"comentario\": \"" + largo + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("la calificacion sin comentario es valida")
        void permiteSoloEstrellas() throws Exception {
            mockMvc.perform(post("/lugares/catedral/resenas")
                            .header("Authorization", "Bearer " + tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"calificacion": 4}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.comentario").doesNotExist());
        }
    }

    // ---------------------------------------------------------------
    //  Editar y borrar
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Editar la propia resena")
    class Editar {

        @Test
        @DisplayName("el autor puede cambiar su nota y queda marcada como editada")
        void editaLaSuya() throws Exception {
            String id = idDe(calificar(tokenAna, "catedral", 5, "Genial"));

            // El sello de edicion se compara con el de creacion; se fuerza la
            // diferencia para no depender de la velocidad de la maquina.
            jdbc.update("UPDATE resena SET created_at = created_at - INTERVAL '1 hour'");

            mockMvc.perform(put("/lugares/catedral/resenas/" + id)
                            .header("Authorization", "Bearer " + tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"calificacion": 2, "comentario": "Lo he pensado mejor."}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.calificacion").value(2))
                    .andExpect(jsonPath("$.editada").value(true));
        }

        @Test
        @DisplayName("tras borrar la propia resena se puede volver a opinar")
        void borrarPermiteOpinarDeNuevo() throws Exception {
            String id = idDe(calificar(tokenAna, "catedral", 5, "Me encanto"));

            mockMvc.perform(delete("/lugares/catedral/resenas/" + id)
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(status().isNoContent());

            // Como la fila se conserva por la baja logica y existe un UNIQUE
            // (usuario, lugar), sin reutilizarla el usuario quedaria vetado
            // para siempre en este lugar. Debe poder escribir otra.
            mockMvc.perform(post("/lugares/catedral/resenas")
                            .header("Authorization", "Bearer " + tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"calificacion": 3, "comentario": "Segunda visita, mas templada."}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.calificacion").value(3));

            // Y sigue habiendo UNA sola fila: se reutilizo, no se duplico.
            assertThat(contar("resena")).isEqualTo(1);
        }

        @Test
        @DisplayName("una resena borrada no se ofrece para editar")
        void borradaNoApareceComoMia() throws Exception {
            String id = idDe(calificar(tokenAna, "catedral", 5, "Me encanto"));

            mockMvc.perform(delete("/lugares/catedral/resenas/" + id)
                    .header("Authorization", "Bearer " + tokenAna));

            mockMvc.perform(get("/lugares/catedral/resenas/mia")
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("nadie puede editar la resena de otra persona")
        void noEditaLaAjena() throws Exception {
            String id = idDe(calificar(tokenAna, "catedral", 5, "Genial"));

            mockMvc.perform(put("/lugares/catedral/resenas/" + id)
                            .header("Authorization", "Bearer " + tokenBruno)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"calificacion": 1, "comentario": "Secuestrada."}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("resena-ajena"));

            // Y la original sigue intacta.
            Short nota = jdbc.queryForObject(
                    "SELECT calificacion FROM resena WHERE id = ?::uuid", Short.class, id);
            assertThat(nota).isEqualTo((short) 5);
        }

        @Test
        @DisplayName("editar una resena oculta NO la vuelve a publicar")
        void editarNoResucita() throws Exception {
            String id = idDe(calificar(tokenAna, "catedral", 5, "Genial"));

            mockMvc.perform(post("/admin/moderacion/resenas/" + id + "/ocultar")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isNoContent());

            mockMvc.perform(put("/lugares/catedral/resenas/" + id)
                            .header("Authorization", "Bearer " + tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"calificacion": 5, "comentario": "Reescrita para colarla."}
                                    """))
                    .andExpect(status().isOk());

            // Si bastara con editar para volver a verse, moderar no serviria.
            String estado = jdbc.queryForObject(
                    "SELECT estado FROM resena WHERE id = ?::uuid", String.class, id);
            assertThat(estado).isEqualTo("OCULTA");
        }
    }

    // ---------------------------------------------------------------
    //  Promedio desde la vista materializada
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Calificacion promedio")
    class Promedio {

        @Test
        @DisplayName("sale de la vista materializada, no de una columna de lugar")
        void vieneDeLaVista() throws Exception {
            calificar(tokenAna, "catedral", 5, null);
            calificar(tokenBruno, "catedral", 3, null);
            refrescarVista();

            mockMvc.perform(get("/lugares").param("idioma", "ES"))
                    .andExpect(jsonPath("$.content[?(@.slug=='catedral')].calificacionPromedio")
                            .value(org.hamcrest.Matchers.contains(4.0)))
                    .andExpect(jsonPath("$.content[?(@.slug=='catedral')].totalResenas")
                            .value(org.hamcrest.Matchers.contains(2)));

            // Y en la tabla lugar no existe ninguna columna de promedio: si
            // alguien la anadiera, este test lo delataria.
            Integer columnas = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_name = 'lugar' AND column_name LIKE '%calificacion%'
                    """, Integer.class);
            assertThat(columnas).isZero();
        }

        @Test
        @DisplayName("una resena oculta deja de contar en el promedio")
        void ocultarBajaElPromedio() throws Exception {
            calificar(tokenAna, "catedral", 5, null);
            String idBaja = idDe(calificar(tokenBruno, "catedral", 1, "No me gusto"));
            refrescarVista();

            Double antes = promedioDe("catedral");
            assertThat(antes).isEqualTo(3.0);

            mockMvc.perform(post("/admin/moderacion/resenas/" + idBaja + "/ocultar")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isNoContent());
            refrescarVista();

            assertThat(promedioDe("catedral"))
                    .as("al ocultar la de 1 estrella solo queda la de 5")
                    .isEqualTo(5.0);
        }

        @Test
        @DisplayName("restaurar una resena la devuelve al promedio")
        void restaurarLaDevuelve() throws Exception {
            calificar(tokenAna, "catedral", 5, null);
            String id = idDe(calificar(tokenBruno, "catedral", 1, null));

            mockMvc.perform(post("/admin/moderacion/resenas/" + id + "/ocultar")
                    .header("Authorization", "Bearer " + tokenAdmin));
            refrescarVista();
            assertThat(promedioDe("catedral")).isEqualTo(5.0);

            mockMvc.perform(post("/admin/moderacion/resenas/" + id + "/restaurar")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isNoContent());
            refrescarVista();

            assertThat(promedioDe("catedral")).isEqualTo(3.0);
        }
    }

    // ---------------------------------------------------------------
    //  Permisos de moderacion (RF-50)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Permisos de moderacion")
    class Permisos {

        @Test
        @DisplayName("un usuario normal no puede abrir la bandeja")
        void usuarioNormalNoModera() throws Exception {
            mockMvc.perform(get("/admin/moderacion/resenas")
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("sin token la bandeja responde 401")
        void sinTokenNoModera() throws Exception {
            mockMvc.perform(get("/admin/moderacion/resenas"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("un usuario normal no puede ocultar resenas ajenas")
        void usuarioNormalNoOculta() throws Exception {
            String id = idDe(calificar(tokenAna, "catedral", 5, null));

            mockMvc.perform(post("/admin/moderacion/resenas/" + id + "/ocultar")
                            .header("Authorization", "Bearer " + tokenBruno))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("la bandeja marca las resenas editadas tras publicarse")
        void bandejaMarcaEditadas() throws Exception {
            String id = idDe(calificar(tokenAna, "catedral", 5, "Original"));
            jdbc.update("UPDATE resena SET created_at = created_at - INTERVAL '1 hour'");

            mockMvc.perform(put("/lugares/catedral/resenas/" + id)
                    .header("Authorization", "Bearer " + tokenAna)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"calificacion": 5, "comentario": "Cambiada despues"}
                            """));

            mockMvc.perform(get("/admin/moderacion/resenas")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].editada").value(true));
        }
    }

    // ---------------------------------------------------------------
    //  Lectura publica
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Lectura publica")
    class Lectura {

        @Test
        @DisplayName("las resenas se leen sin cuenta")
        void listaPublica() throws Exception {
            calificar(tokenAna, "catedral", 5, "Preciosa");

            mockMvc.perform(get("/lugares/catedral/resenas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].comentario").value("Preciosa"));
        }

        @Test
        @DisplayName("una resena oculta desaparece de la lista publica")
        void ocultaNoSeLista() throws Exception {
            String id = idDe(calificar(tokenAna, "catedral", 5, "Preciosa"));

            mockMvc.perform(post("/admin/moderacion/resenas/" + id + "/ocultar")
                    .header("Authorization", "Bearer " + tokenAdmin));

            mockMvc.perform(get("/lugares/catedral/resenas"))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // ---------------------------------------------------------------
    //  Ayudantes
    // ---------------------------------------------------------------

    private MvcResult calificar(String token, String slug, int nota, String comentario)
            throws Exception {
        String cuerpo = comentario == null
                ? "{\"calificacion\": %d}".formatted(nota)
                : "{\"calificacion\": %d, \"comentario\": \"%s\"}".formatted(nota, comentario);

        return mockMvc.perform(post("/lugares/" + slug + "/resenas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String idDe(MvcResult resultado) throws Exception {
        String json = resultado.getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.id");
    }

    private Double promedioDe(String slug) {
        return jdbc.queryForObject("""
                SELECT e.calificacion_promedio FROM estadistica_lugar e
                JOIN lugar l ON l.id = e.lugar_id WHERE l.slug = ?
                """, Double.class, slug);
    }

    /**
     * Refresca la vista a mano.
     *
     * <p>En produccion lo hace el carril rapido en 30 s como mucho; en un test
     * esperar seria absurdo, asi que se fuerza y se comprueba el resultado.</p>
     */
    private void refrescarVista() {
        jdbc.execute("REFRESH MATERIALIZED VIEW estadistica_lugar");
    }

    private Integer contar(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    private void crearLugar(String slug, UUID categoriaId, String nombre) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, estado)
                SELECT uuid_generar_v7(), ?, ?, d.id,
                       ST_SetSRID(ST_MakePoint(-74.2236, -13.1588), 4326), 'PUBLICADO'
                FROM distrito d WHERE d.codigo='050101' RETURNING id
                """, UUID.class, slug, categoriaId);

        jdbc.update("INSERT INTO lugar_traduccion (lugar_id, idioma, nombre) VALUES (?, 'es', ?)",
                id, nombre);
    }

    private void limpiarClaves(String patron) {
        var claves = redis.keys(patron);
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
    }

    /**
     * Registra y luego inicia sesion.
     *
     * <p>{@code /auth/register} devuelve el usuario creado, no un token: el
     * access token solo lo emite {@code /auth/login}.</p>
     */
    private String registrarYObtenerToken(String email) throws Exception {
        String nombre = email.substring(0, 1).toUpperCase() + email.substring(1, email.indexOf('@'));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Yachay2026Dev", "nombre": "%s"}
                                """.formatted(email, nombre)))
                .andExpect(status().isCreated());

        return iniciarSesion(email);
    }

    private String iniciarSesion(String email) throws Exception {
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

    private String crearAdminYObtenerToken() throws Exception {
        registrarYObtenerToken("jefa@yachay.pe");
        jdbc.update("""
                UPDATE usuario SET rol_id = (SELECT id FROM rol WHERE nombre='ADMIN')
                WHERE email = 'jefa@yachay.pe'
                """);
        // El rol viaja dentro del JWT, asi que hay que pedir uno nuevo despues
        // de cambiarlo: el anterior sigue diciendo USUARIO.
        return iniciarSesion("jefa@yachay.pe");
    }
}
