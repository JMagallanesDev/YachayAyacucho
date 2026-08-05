package com.huamanga.tourism.lugar;

import com.huamanga.tourism.soporte.BasePostgis;
import com.jayway.jsonpath.JsonPath;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD de lugares patrimoniales (RF-47).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CRUD de lugares")
class LugarCrudTest extends BasePostgis {

    private static final String PASSWORD = "Yachay2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private UUID categoriaId;
    private UUID distritoId;
    private String tokenAdmin;
    private String tokenUsuario;

    @BeforeEach
    void preparar() throws Exception {
        jdbc.execute("DELETE FROM horario_lugar");
        jdbc.execute("DELETE FROM lugar_traduccion");
        jdbc.execute("DELETE FROM lugar");
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");
        var claves = redis.keys("rate:*");
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }

        categoriaId = jdbc.queryForObject(
                "SELECT id FROM categoria_lugar WHERE codigo = 'IGLESIAS'", UUID.class);
        distritoId = jdbc.queryForObject(
                "SELECT id FROM distrito WHERE codigo = '050101'", UUID.class);

        tokenAdmin = tokenDe("jefa@yachay.pe", true);
        tokenUsuario = tokenDe("turista@yachay.pe", false);
    }

    // ---------------------------------------------------------------
    //  Permisos (RF-47, RNF-16)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Permisos")
    class Permisos {

        @Test
        @DisplayName("crear sin autenticar devuelve 401")
        void crearSinTokenDevuelve401() throws Exception {
            mockMvc.perform(post("/lugares")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido("sin-token")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("crear con rol USUARIO devuelve 403, no 401")
        void crearConUsuarioDevuelve403() throws Exception {
            mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenUsuario)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido("sin-permiso")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("acceso-denegado"));
        }

        @Test
        @DisplayName("editar y borrar tambien exigen rol ADMIN")
        void editarYBorrarExigenAdmin() throws Exception {
            String id = crearComoAdmin("protegido");

            mockMvc.perform(put("/lugares/" + id)
                            .header("Authorization", "Bearer " + tokenUsuario)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido("protegido")))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/lugares/" + id)
                            .header("Authorization", "Bearer " + tokenUsuario))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("el listado publico es accesible sin autenticar (RF-34)")
        void listadoEsPublico() throws Exception {
            mockMvc.perform(get("/lugares")).andExpect(status().isOk());
        }
    }

    // ---------------------------------------------------------------
    //  Guardado transaccional
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Guardado")
    class Guardado {

        @Test
        @DisplayName("crea el lugar con sus traducciones y horarios en una sola operacion")
        void creaTodoJunto() throws Exception {
            mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido("catedral")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.slug").value("catedral"))
                    .andExpect(jsonPath("$.nombre").value("Catedral de Ayacucho"))
                    .andExpect(jsonPath("$.horarios.length()").value(2))
                    .andExpect(jsonPath("$.categoria.nombre").value("Iglesias y templos"))
                    .andExpect(jsonPath("$.distrito.provincia").value("Huamanga"));

            assertThat(contar("lugar")).isEqualTo(1);
            assertThat(contar("lugar_traduccion")).isEqualTo(2);
            assertThat(contar("horario_lugar")).isEqualTo(2);
        }

        @Test
        @DisplayName("si la validacion falla no queda nada a medias")
        void noDejaNadaAMedias() throws Exception {
            // Coordenadas de Lima: validas como punto, invalidas para este sistema.
            mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido("fallido").replace("-74.2236", "-77.0428")))
                    .andExpect(status().isBadRequest());

            assertThat(contar("lugar")).isZero();
            assertThat(contar("lugar_traduccion")).isZero();
            assertThat(contar("horario_lugar")).isZero();
        }

        @Test
        @DisplayName("al actualizar reemplaza traducciones y horarios sin duplicarlos")
        void actualizarReemplazaColecciones() throws Exception {
            String id = crearComoAdmin("editable");

            // Se envia una sola traduccion y un solo horario.
            String cuerpoReducido = """
                    {
                      "slug": "editable",
                      "categoriaId": "%s",
                      "distritoId": "%s",
                      "longitud": -74.2236,
                      "latitud": -13.1588,
                      "estado": "PUBLICADO",
                      "traducciones": [
                        {"idioma": "ES", "nombre": "Nombre corregido", "descripcion": "Nueva descripcion"}
                      ],
                      "horarios": [
                        {"diaSemana": 3, "horaApertura": "10:00", "horaCierre": "16:00", "cerrado": false}
                      ]
                    }
                    """.formatted(categoriaId, distritoId);

            mockMvc.perform(put("/lugares/" + id)
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoReducido))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Nombre corregido"))
                    .andExpect(jsonPath("$.horarios.length()").value(1));

            // Lo que sobraba se borro; no se acumulo.
            assertThat(contar("lugar_traduccion")).isEqualTo(1);
            assertThat(contar("horario_lugar")).isEqualTo(1);
        }

        @Test
        @DisplayName("rechaza un slug repetido con 409")
        void rechazaSlugDuplicado() throws Exception {
            crearComoAdmin("repetido");

            mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido("repetido")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("slug-duplicado"));
        }

        @Test
        @DisplayName("la hora guardada es la misma que se envio, sin desplazamiento horario")
        void horariosSinDesfase() throws Exception {
            // Regresion. `hibernate.jdbc.time_zone: UTC` es necesario para los
            // instantes, pero Hibernate lo aplicaba tambien a las columnas TIME,
            // que aqui son hora de pared. Con la JVM en Lima, un horario enviado
            // como 09:00 acababa almacenado como 14:00 y se leia otra vez como
            // 09:00: la aplicacion parecia coherente consigo misma, pero el dato
            // en la base era falso y no coincidia con el cargado por SQL.
            //
            // Por eso este test mira la COLUMNA, no la respuesta del API: una
            // asercion sobre el JSON pasaba igual con el fallo presente.
            crearComoAdmin("horario-fiel");

            String apertura = jdbc.queryForObject(
                    "SELECT hora_apertura::text FROM horario_lugar ORDER BY hora_apertura LIMIT 1",
                    String.class);

            assertThat(apertura)
                    .as("la hora de pared no debe convertirse entre zonas")
                    .startsWith("09:00");
        }

        @Test
        @DisplayName("el borrado es logico: la fila se conserva")
        void borradoEsLogico() throws Exception {
            String id = crearComoAdmin("por-borrar");

            mockMvc.perform(delete("/lugares/" + id)
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isNoContent());

            // Para el API ya no existe...
            mockMvc.perform(get("/lugares/por-borrar")).andExpect(status().isNotFound());
            // ...pero la fila sigue ahi con su marca de baja.
            Integer conMarca = jdbc.queryForObject(
                    "SELECT count(*) FROM lugar WHERE deleted_at IS NOT NULL", Integer.class);
            assertThat(conMarca).isEqualTo(1);
        }
    }

    // ---------------------------------------------------------------
    //  Validacion
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Validacion")
    class Validacion {

        @Test
        @DisplayName("rechaza coordenadas fuera de Ayacucho (RF-22b)")
        void rechazaCoordenadasFuera() throws Exception {
            mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido("lima").replace("-77.0", "-77.0")
                                    .replace("-74.2236", "-77.0428")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.longitud").exists());
        }

        @Test
        @DisplayName("exige la traduccion al espanol")
        void exigeEspanol() throws Exception {
            String soloIngles = """
                    {
                      "slug": "solo-ingles",
                      "categoriaId": "%s",
                      "distritoId": "%s",
                      "longitud": -74.2236,
                      "latitud": -13.1588,
                      "estado": "PUBLICADO",
                      "traducciones": [{"idioma": "EN", "nombre": "English only"}],
                      "horarios": []
                    }
                    """.formatted(categoriaId, distritoId);

            mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(soloIngles))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.traducciones").exists());
        }

        @Test
        @DisplayName("rechaza dos traducciones al mismo idioma")
        void rechazaIdiomaDuplicado() throws Exception {
            String duplicado = """
                    {
                      "slug": "duplicado-idioma",
                      "categoriaId": "%s",
                      "distritoId": "%s",
                      "longitud": -74.2236,
                      "latitud": -13.1588,
                      "estado": "PUBLICADO",
                      "traducciones": [
                        {"idioma": "ES", "nombre": "Primera"},
                        {"idioma": "ES", "nombre": "Segunda"}
                      ],
                      "horarios": []
                    }
                    """.formatted(categoriaId, distritoId);

            mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(duplicado))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("rechaza franjas horarias solapadas el mismo dia")
        void rechazaHorariosSolapados() throws Exception {
            String solapado = cuerpoValido("solapado")
                    .replace("\"diaSemana\": 1, \"horaApertura\": \"15:00\", \"horaCierre\": \"18:00\"",
                            "\"diaSemana\": 1, \"horaApertura\": \"12:00\", \"horaCierre\": \"18:00\"");

            mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(solapado))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.horarios").exists());
        }

        @Test
        @DisplayName("rechaza una apertura posterior al cierre")
        void rechazaRangoInvertido() throws Exception {
            String invertido = cuerpoValido("invertido")
                    .replace("\"horaApertura\": \"09:00\", \"horaCierre\": \"13:00\"",
                            "\"horaApertura\": \"18:00\", \"horaCierre\": \"09:00\"");

            mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invertido))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("rechaza un slug con formato invalido")
        void rechazaSlugInvalido() throws Exception {
            mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido("Con Mayusculas Y Espacios")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.slug").exists());
        }
    }

    // ---------------------------------------------------------------
    //  Idiomas
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Idiomas")
    class Idiomas {

        @Test
        @DisplayName("devuelve el contenido en el idioma pedido")
        void devuelveElIdiomaPedido() throws Exception {
            crearComoAdmin("bilingue");

            mockMvc.perform(get("/lugares/bilingue").param("idioma", "ES"))
                    .andExpect(jsonPath("$.nombre").value("Catedral de Ayacucho"))
                    .andExpect(jsonPath("$.traduccionPorDefecto").value(false));

            mockMvc.perform(get("/lugares/bilingue").param("idioma", "EN"))
                    .andExpect(jsonPath("$.nombre").value("Ayacucho Cathedral"))
                    .andExpect(jsonPath("$.idioma").value("EN"));
        }

        @Test
        @DisplayName("cae al espanol cuando falta la traduccion y lo indica")
        void caeAlEspanolSiFalta() throws Exception {
            String soloEspanol = """
                    {
                      "slug": "solo-es",
                      "categoriaId": "%s",
                      "distritoId": "%s",
                      "longitud": -74.2236,
                      "latitud": -13.1588,
                      "estado": "PUBLICADO",
                      "traducciones": [{"idioma": "ES", "nombre": "Solo en espanol"}],
                      "horarios": []
                    }
                    """.formatted(categoriaId, distritoId);

            mockMvc.perform(post("/lugares")
                    .header("Authorization", "Bearer " + tokenAdmin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(soloEspanol));

            mockMvc.perform(get("/lugares/solo-es").param("idioma", "EN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Solo en espanol"))
                    .andExpect(jsonPath("$.idioma").value("ES"))
                    // El cliente sabe que recibio el fallback, no una traduccion.
                    .andExpect(jsonPath("$.traduccionPorDefecto").value(true));
        }

        @Test
        @DisplayName("los errores de validacion salen en el idioma de Accept-Language")
        void erroresTraducidos() throws Exception {
            String invalido = cuerpoValido("x").replace("-74.2236", "-77.0428");

            String enEspanol = mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .header("Accept-Language", "es")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalido))
                    .andReturn().getResponse().getContentAsString();

            String enIngles = mockMvc.perform(post("/lugares")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .header("Accept-Language", "en")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalido))
                    .andReturn().getResponse().getContentAsString();

            assertThat(JsonPath.<String>read(enEspanol, "$.errores.longitud"))
                    .isEqualTo("Las coordenadas quedan fuera de la region Ayacucho");
            assertThat(JsonPath.<String>read(enIngles, "$.errores.longitud"))
                    .isEqualTo("The coordinates fall outside the Ayacucho region");
            assertThat(JsonPath.<String>read(enIngles, "$.title")).isEqualTo("Invalid data");
        }
    }

    // ---------------------------------------------------------------
    //  Visibilidad de borradores
    // ---------------------------------------------------------------

    @Test
    @DisplayName("un borrador es 404 para el publico y visible para el administrador")
    void borradorSoloVisibleParaAdmin() throws Exception {
        mockMvc.perform(post("/lugares")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoValido("en-borrador").replace("\"PUBLICADO\"", "\"BORRADOR\"")));

        // 404 y no 403: un 403 confirmaria que el borrador existe.
        mockMvc.perform(get("/lugares/en-borrador")).andExpect(status().isNotFound());

        mockMvc.perform(get("/lugares/en-borrador")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("el listado publico no incluye borradores")
    void listadoExcluyeBorradores() throws Exception {
        crearComoAdmin("publicado-visible");
        mockMvc.perform(post("/lugares")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoValido("oculto").replace("\"PUBLICADO\"", "\"BORRADOR\"")));

        mockMvc.perform(get("/lugares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("publicado-visible"));
    }

    // ---------------------------------------------------------------
    //  Utilidades
    // ---------------------------------------------------------------

    private String cuerpoValido(String slug) {
        return """
                {
                  "slug": "%s",
                  "categoriaId": "%s",
                  "distritoId": "%s",
                  "longitud": -74.2236,
                  "latitud": -13.1588,
                  "direccion": "Plaza Mayor de Huamanga",
                  "precioEntradaPen": 0.00,
                  "duracionVisitaMin": 45,
                  "tieneBanos": true,
                  "accesibleSillaRuedas": true,
                  "aptoNinos": true,
                  "estado": "PUBLICADO",
                  "traducciones": [
                    {"idioma": "ES", "nombre": "Catedral de Ayacucho", "descripcion": "Templo principal"},
                    {"idioma": "EN", "nombre": "Ayacucho Cathedral", "descripcion": "Main temple"}
                  ],
                  "horarios": [
                    {"diaSemana": 1, "horaApertura": "09:00", "horaCierre": "13:00", "cerrado": false},
                    {"diaSemana": 1, "horaApertura": "15:00", "horaCierre": "18:00", "cerrado": false}
                  ]
                }
                """.formatted(slug, categoriaId, distritoId);
    }

    private String crearComoAdmin(String slug) throws Exception {
        String cuerpo = mockMvc.perform(post("/lugares")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoValido(slug)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.id");
    }

    private String tokenDe(String email, boolean admin) throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","nombre":"Persona"}
                        """.formatted(email, PASSWORD)));

        if (admin) {
            jdbc.update("""
                    UPDATE usuario SET rol_id = (SELECT id FROM rol WHERE nombre = 'ADMIN')
                    WHERE email = ?
                    """, email);
        }

        String cuerpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.accessToken");
    }

    private Integer contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }
}
