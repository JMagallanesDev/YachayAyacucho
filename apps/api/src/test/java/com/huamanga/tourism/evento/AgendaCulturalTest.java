package com.huamanga.tourism.evento;

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

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Agenda cultural: calendario, ficha, cruce con el viaje y clonado anual
 * (RF-79, RF-80, RF-84, RF-84b, RF-85, RF-86).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Agenda cultural")
class AgendaCulturalTest extends BasePostgis {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redis;

    private UUID distrito;
    private String tokenAdmin;
    private String tokenVisitante;

    @BeforeEach
    void preparar() throws Exception {
        // Los contenedores son singletons de toda la suite, asi que el contador
        // del rate limit llega con lo que hayan gastado las clases anteriores.
        // Sin esto, el registro de usuarios de este @BeforeEach recibe un 429 y
        // fallan los treinta tests por una razon que no tiene que ver con ellos.
        limpiar("rate:*");
        limpiar("antispam:*");

        jdbc.execute("DELETE FROM evento_traduccion");
        jdbc.execute("DELETE FROM evento");
        jdbc.execute("DELETE FROM refresh_token");
        // La bitacora de administracion (RF-56, Bloque 10) referencia al
        // usuario, asi que hay que vaciarla antes de borrar cuentas.
        jdbc.execute("DELETE FROM registro_actividad");
        jdbc.execute("DELETE FROM usuario");

        distrito = jdbc.queryForObject(
                "SELECT id FROM distrito WHERE codigo = '050101'", UUID.class);

        tokenVisitante = registrar("turista@yachay.pe");
        tokenAdmin = crearAdmin();
    }

    // ---------------------------------------------------------------
    //  Calendario mensual (RF-79)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Calendario mensual")
    class Calendario {

        @Test
        @DisplayName("solo devuelve eventos publicados")
        void soloPublicados() throws Exception {
            crear("2026-09-10", "2026-09-10", "CULTURAL", "PUBLICADO", false, "Publicado");
            crear("2026-09-11", "2026-09-11", "CULTURAL", "BORRADOR", false, "Borrador");
            crear("2026-09-12", "2026-09-12", "CULTURAL", "CANCELADO", false, "Cancelado");

            mockMvc.perform(get("/eventos/calendario?anio=2026&mes=9&idioma=ES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nombre").value("Publicado"));
        }

        /**
         * El caso que el usuario pidio explicitamente: una fiesta de varios dias
         * tiene que estar en el calendario todos los dias que dura, incluso si
         * empezo el mes anterior.
         */
        @Test
        @DisplayName("una fiesta que cruza el cambio de mes sale en LOS DOS meses")
        void fiestaACaballoEntreDosMeses() throws Exception {
            crear("2026-03-27", "2026-04-05", "RELIGIOSO", "PUBLICADO", true, "Semana Santa");

            mockMvc.perform(get("/eventos/calendario?anio=2026&mes=3&idioma=ES"))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].duracionDias").value(10));

            mockMvc.perform(get("/eventos/calendario?anio=2026&mes=4&idioma=ES"))
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("no devuelve el mes anterior ni el siguiente")
        void noSeCuelanLosMesesVecinos() throws Exception {
            crear("2026-08-31", "2026-08-31", "CULTURAL", "PUBLICADO", false, "Agosto");
            crear("2026-10-01", "2026-10-01", "CULTURAL", "PUBLICADO", false, "Octubre");

            mockMvc.perform(get("/eventos/calendario?anio=2026&mes=9&idioma=ES"))
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("febrero de un bisiesto llega hasta el dia 29")
        void febreroBisiesto() throws Exception {
            crear("2028-02-29", "2028-02-29", "CULTURAL", "PUBLICADO", false, "Ultimo dia");

            mockMvc.perform(get("/eventos/calendario?anio=2028&mes=2&idioma=ES"))
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("filtra por tipo (RF-85)")
        void filtraPorTipo() throws Exception {
            crear("2026-09-10", "2026-09-10", "RELIGIOSO", "PUBLICADO", false, "Procesion");
            crear("2026-09-11", "2026-09-11", "GASTRONOMICO", "PUBLICADO", false, "Feria");

            mockMvc.perform(get("/eventos/calendario?anio=2026&mes=9&tipo=RELIGIOSO&idioma=ES"))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nombre").value("Procesion"));
        }

        @Test
        @DisplayName("se consulta sin cuenta (RF-34)")
        void esPublico() throws Exception {
            mockMvc.perform(get("/eventos/calendario?anio=2026&mes=9"))
                    .andExpect(status().isOk());
        }
    }

    // ---------------------------------------------------------------
    //  Proximos eventos (RF-84)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Proximos eventos")
    class Proximos {

        @Test
        @DisplayName("una fiesta EN CURSO sigue siendo un proximo evento")
        void laFiestaEnCursoNoDesaparece() throws Exception {
            LocalDate hoy = LocalDate.now();
            crear(hoy.minusDays(2).toString(), hoy.plusDays(2).toString(),
                    "CULTURAL", "PUBLICADO", false, "En marcha");

            mockMvc.perform(get("/eventos/proximos?idioma=ES"))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nombre").value("En marcha"));
        }

        @Test
        @DisplayName("los eventos ya terminados no aparecen")
        void losPasadosNo() throws Exception {
            LocalDate hoy = LocalDate.now();
            crear(hoy.minusDays(10).toString(), hoy.minusDays(9).toString(),
                    "CULTURAL", "PUBLICADO", false, "Ya paso");

            mockMvc.perform(get("/eventos/proximos?idioma=ES"))
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("respeta el limite pedido")
        void respetaElLimite() throws Exception {
            LocalDate hoy = LocalDate.now();
            for (int i = 1; i <= 4; i++) {
                crear(hoy.plusDays(i).toString(), hoy.plusDays(i).toString(),
                        "CULTURAL", "PUBLICADO", false, "Evento " + i);
            }

            mockMvc.perform(get("/eventos/proximos?limite=2&idioma=ES"))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].nombre").value("Evento 1"));
        }
    }

    // ---------------------------------------------------------------
    //  Ficha (RF-80)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Ficha de evento")
    class Ficha {

        @Test
        @DisplayName("devuelve el contenido con su clima")
        void conClima() throws Exception {
            UUID id = crear("2027-05-10", "2027-05-12", "CULTURAL", "PUBLICADO", false, "Festival");

            mockMvc.perform(get("/eventos/" + id + "?idioma=ES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evento.nombre").value("Festival"))
                    .andExpect(jsonPath("$.evento.duracionDias").value(3))
                    // Un evento a un anio vista nunca tiene pronostico, y eso no
                    // es un fallo: el contrato obliga a decir por que.
                    .andExpect(jsonPath("$.clima.estado").value("FUERA_DE_ALCANCE"))
                    .andExpect(jsonPath("$.clima.temporada").value("SECA"));
        }

        @Test
        @DisplayName("un borrador devuelve 404 a quien no es administrador")
        void borradorEsInvisible() throws Exception {
            UUID id = crear("2027-05-10", "2027-05-10", "CULTURAL", "BORRADOR", false, "Secreto");

            mockMvc.perform(get("/eventos/" + id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("cae al espanol si no hay traduccion inglesa")
        void fallbackAlEspanol() throws Exception {
            UUID id = crear("2027-05-10", "2027-05-10", "CULTURAL", "PUBLICADO", false, "Solo en espanol");

            mockMvc.perform(get("/eventos/" + id + "?idioma=EN"))
                    .andExpect(jsonPath("$.evento.idioma").value("ES"))
                    .andExpect(jsonPath("$.evento.traduccionSustituta").value(true));
        }
    }

    // ---------------------------------------------------------------
    //  Clonado anual (RF-86)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Clonado anual")
    class Clonado {

        @Test
        @DisplayName("copia la plantilla pero NO la fecha vieja, y nace en borrador")
        void copiaLaPlantillaNoLaFecha() throws Exception {
            UUID original = crear("2026-03-27", "2026-04-05", "RELIGIOSO", "PUBLICADO",
                    true, "Semana Santa de Ayacucho");

            MvcResult resultado = mockMvc.perform(post("/admin/eventos/" + original + "/clonar?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"anio": 2027, "fechaInicio": "2027-03-19", "fechaFin": "2027-03-28"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.evento.nombre").value("Semana Santa de Ayacucho"))
                    .andExpect(jsonPath("$.evento.estado").value("BORRADOR"))
                    .andExpect(jsonPath("$.evento.fechaInicio").value("2027-03-19"))
                    .andExpect(jsonPath("$.evento.fechaFin").value("2027-03-28"))
                    .andExpect(jsonPath("$.eventoOrigenId").value(original.toString()))
                    .andReturn();

            UUID clon = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                    resultado.getResponse().getContentAsString(), "$.evento.id"));

            // El original no se toca: sigue publicado y con sus fechas.
            assertThat(fecha(original, "fecha_inicio")).isEqualTo(LocalDate.of(2026, 3, 27));
            assertThat(clon).isNotEqualTo(original);
        }

        /**
         * Lo mas facil de estropear al clonar: reutilizar las instancias de las
         * traducciones. Como su clave primaria incluye el evento, Hibernate no
         * las copiaria sino que las MOVERIA, y la edicion anterior se quedaria
         * sin texto.
         */
        @Test
        @DisplayName("las traducciones se copian como filas nuevas, sin vaciar el original")
        void traduccionesComoFilasNuevas() throws Exception {
            UUID original = crear("2026-12-09", "2026-12-09", "CIVICO", "PUBLICADO",
                    true, "Batalla de Ayacucho");

            mockMvc.perform(post("/admin/eventos/" + original + "/clonar?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"anio\": 2027}"))
                    .andExpect(status().isCreated());

            assertThat(contarTraducciones(original))
                    .as("el original conserva su texto")
                    .isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM evento_traduccion", Integer.class))
                    .as("hay dos juegos de traducciones, uno por edicion")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("sin fechas explicitas, desplaza el anio y CONSERVA la duracion")
        void conservaLaDuracion() throws Exception {
            // Del 27 de febrero al 2 de marzo de 2027: 4 dias. Al clonar a 2028,
            // que es bisiesto, desplazar las dos fechas por separado daria 5.
            UUID original = crear("2027-02-27", "2027-03-02", "CULTURAL", "PUBLICADO",
                    true, "Carnaval");

            mockMvc.perform(post("/admin/eventos/" + original + "/clonar?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"anio\": 2028}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.evento.fechaInicio").value("2028-02-27"))
                    .andExpect(jsonPath("$.evento.duracionDias").value(4))
                    .andExpect(jsonPath("$.evento.fechaFin").value("2028-03-01"));
        }

        @Test
        @DisplayName("el 29 de febrero cae al 28 en un anio normal")
        void veintinueveDeFebrero() throws Exception {
            UUID original = crear("2028-02-29", "2028-02-29", "CULTURAL", "PUBLICADO",
                    true, "Fiesta del bisiesto");

            mockMvc.perform(post("/admin/eventos/" + original + "/clonar?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"anio\": 2029}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.evento.fechaInicio").value("2029-02-28"));
        }

        @Test
        @DisplayName("clonar dos veces al mismo anio devuelve 409")
        void noSeClonaDosVeces() throws Exception {
            UUID original = crear("2026-12-09", "2026-12-09", "CIVICO", "PUBLICADO",
                    true, "Batalla");

            mockMvc.perform(post("/admin/eventos/" + original + "/clonar")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"anio\": 2027}"))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/admin/eventos/" + original + "/clonar")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"anio\": 2027}"))
                    .andExpect(status().isConflict());

            assertThat(contarEventos()).isEqualTo(2);
        }

        @Test
        @DisplayName("un evento que NO se repite cada anio no se puede clonar")
        void soloLosRecurrentes() throws Exception {
            UUID unico = crear("2026-09-10", "2026-09-10", "CULTURAL", "PUBLICADO",
                    false, "Concierto de una vez");

            mockMvc.perform(post("/admin/eventos/" + unico + "/clonar")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"anio\": 2027}"))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("una fecha de otro anio distinto al pedido se rechaza")
        void fechaIncoherente() throws Exception {
            UUID original = crear("2026-12-09", "2026-12-09", "CIVICO", "PUBLICADO",
                    true, "Batalla");

            mockMvc.perform(post("/admin/eventos/" + original + "/clonar")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"anio": 2027, "fechaInicio": "2029-12-09"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("clonar exige rol ADMIN")
        void soloAdmin() throws Exception {
            UUID original = crear("2026-12-09", "2026-12-09", "CIVICO", "PUBLICADO",
                    true, "Batalla");

            mockMvc.perform(post("/admin/eventos/" + original + "/clonar")
                            .header("Authorization", "Bearer " + tokenVisitante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"anio\": 2027}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    //  Durante mi visita (RF-84b)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Durante mi visita")
    class DuranteMiVisita {

        @Test
        @DisplayName("incluye la fiesta que empezo ANTES de que llegara el turista")
        void solapeConEventoYaEmpezado() throws Exception {
            // El caso que mas se olvida: llega el miercoles y la fiesta arranco
            // el lunes. Filtrar por "empieza dentro del viaje" la escondería.
            crear("2026-09-07", "2026-09-12", "RELIGIOSO", "PUBLICADO", false, "Fiesta patronal");

            mockMvc.perform(get("/eventos/durante-mi-visita?desde=2026-09-09&hasta=2026-09-11&idioma=ES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventos.length()").value(1))
                    .andExpect(jsonPath("$.dias.length()").value(3))
                    .andExpect(jsonPath("$.dias[0].eventoIds.length()").value(1));
        }

        @Test
        @DisplayName("un evento fuera del viaje por un dia no entra")
        void limiteJustoFuera() throws Exception {
            crear("2026-09-12", "2026-09-12", "CULTURAL", "PUBLICADO", false, "Un dia tarde");

            mockMvc.perform(get("/eventos/durante-mi-visita?desde=2026-09-09&hasta=2026-09-11&idioma=ES"))
                    .andExpect(jsonPath("$.eventos.length()").value(0));
        }

        @Test
        @DisplayName("cada dia del viaje trae su clima, sin errores")
        void cadaDiaTieneClima() throws Exception {
            mockMvc.perform(get("/eventos/durante-mi-visita?desde=2027-09-09&hasta=2027-09-11&idioma=ES"))
                    .andExpect(jsonPath("$.dias.length()").value(3))
                    // A un anio vista no hay pronostico, y aun asi cada dia
                    // responde algo con sentido.
                    .andExpect(jsonPath("$.dias[0].clima.estado").value("FUERA_DE_ALCANCE"))
                    .andExpect(jsonPath("$.dias[2].clima.temporada").value("SECA"));
        }

        @Test
        @DisplayName("un regreso anterior a la llegada se rechaza")
        void rangoInvertido() throws Exception {
            mockMvc.perform(get("/eventos/durante-mi-visita?desde=2026-09-11&hasta=2026-09-09"))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("un viaje de mas de 30 dias se rechaza")
        void viajeDemasiadoLargo() throws Exception {
            mockMvc.perform(get("/eventos/durante-mi-visita?desde=2026-09-01&hasta=2026-12-01"))
                    .andExpect(status().isUnprocessableContent());
        }
    }

    // ---------------------------------------------------------------
    //  Gestion (RF-86)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Gestion")
    class Gestion {

        @Test
        @DisplayName("crear exige rol ADMIN")
        void crearSoloAdmin() throws Exception {
            mockMvc.perform(post("/admin/eventos")
                            .header("Authorization", "Bearer " + tokenVisitante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("2026-09-10", "2026-09-10", "CULTURAL",
                                    "PUBLICADO", false, "Intruso")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("sin autenticar devuelve 401")
        void sinAutenticar() throws Exception {
            mockMvc.perform(post("/admin/eventos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("2026-09-10", "2026-09-10", "CULTURAL",
                                    "PUBLICADO", false, "Anonimo")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("la fecha de fin no puede ser anterior a la de inicio")
        void fechasInvertidas() throws Exception {
            mockMvc.perform(post("/admin/eventos")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("2026-09-10", "2026-09-01", "CULTURAL",
                                    "PUBLICADO", false, "Al reves")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("un evento de mas de 31 dias se rechaza: casi siempre es un anio mal tecleado")
        void duracionAbsurda() throws Exception {
            mockMvc.perform(post("/admin/eventos")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("2026-09-10", "2027-09-10", "CULTURAL",
                                    "PUBLICADO", false, "Un anio entero")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("sin version en espanol se rechaza")
        void sinEspanol() throws Exception {
            mockMvc.perform(post("/admin/eventos")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"distritoId": "%s", "tipo": "CULTURAL",
                                     "fechaInicio": "2026-09-10", "fechaFin": "2026-09-10",
                                     "recurrenteAnual": false, "estado": "PUBLICADO",
                                     "traducciones": [{"idioma": "EN", "nombre": "English only"}]}
                                    """.formatted(distrito)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("editar reemplaza el evento completo")
        void editar() throws Exception {
            UUID id = crear("2026-09-10", "2026-09-10", "CULTURAL", "BORRADOR", false, "Antes");

            mockMvc.perform(put("/admin/eventos/" + id + "?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("2026-09-11", "2026-09-13", "MUSICAL",
                                    "PUBLICADO", true, "Despues")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evento.nombre").value("Despues"))
                    .andExpect(jsonPath("$.evento.tipo").value("MUSICAL"))
                    .andExpect(jsonPath("$.evento.duracionDias").value(3));

            assertThat(contarTraducciones(id))
                    .as("editar no deja traducciones huerfanas")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("dar de baja es logico: la fila se conserva")
        void bajaLogica() throws Exception {
            UUID id = crear("2026-09-10", "2026-09-10", "CULTURAL", "PUBLICADO", false, "A borrar");

            mockMvc.perform(delete("/admin/eventos/" + id)
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/eventos/" + id))
                    .andExpect(status().isNotFound());

            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM evento WHERE deleted_at IS NOT NULL", Integer.class))
                    .as("la fila sigue ahi para auditoria")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("la bandeja del admin si muestra los borradores")
        void bandejaConBorradores() throws Exception {
            crear("2026-09-10", "2026-09-10", "CULTURAL", "BORRADOR", false, "Borrador");

            mockMvc.perform(get("/admin/eventos?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    // ---------------------------------------------------------------
    //  Ayudas
    // ---------------------------------------------------------------

    private String cuerpo(String inicio, String fin, String tipo, String estado,
                          boolean recurrente, String nombre) {
        return """
                {"distritoId": "%s", "tipo": "%s",
                 "fechaInicio": "%s", "fechaFin": "%s",
                 "recurrenteAnual": %s, "estado": "%s",
                 "traducciones": [{"idioma": "ES", "nombre": "%s",
                                   "descripcion": "Descripcion de prueba",
                                   "organizador": "Municipalidad"}]}
                """.formatted(distrito, tipo, inicio, fin, recurrente, estado, nombre);
    }

    private UUID crear(String inicio, String fin, String tipo, String estado,
                       boolean recurrente, String nombre) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/admin/eventos?idioma=ES")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(inicio, fin, tipo, estado, recurrente, nombre)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                resultado.getResponse().getContentAsString(), "$.evento.id"));
    }

    private LocalDate fecha(UUID id, String columna) {
        return jdbc.queryForObject(
                "SELECT " + columna + " FROM evento WHERE id = ?", LocalDate.class, id);
    }

    private Integer contarTraducciones(UUID eventoId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM evento_traduccion WHERE evento_id = ?", Integer.class, eventoId);
    }

    private Integer contarEventos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM evento", Integer.class);
    }

    private void limpiar(String patron) {
        var claves = redis.keys(patron);
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
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
}
