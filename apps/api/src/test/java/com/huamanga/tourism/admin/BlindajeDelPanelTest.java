package com.huamanga.tourism.admin;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Blindaje del panel de administracion (RNF-16).
 *
 * <p><strong>Este es el test importante del Bloque 10, y su valor esta en que
 * no hay que mantenerlo.</strong> No comprueba una lista de rutas que alguien
 * escribio a mano —esa lista envejece el mismo dia que se anade un endpoint
 * nuevo—, sino que le pide a Spring el mapa de <em>todos</em> los handlers que
 * tiene registrados, se queda con los que cuelgan de {@code /admin}, y los
 * ataca uno por uno.</p>
 *
 * <p>Es decir: el dia que alguien anada {@code /admin/loquesea} sin protegerlo,
 * este test lo descubre solo. La garantia deja de depender de que nadie se
 * olvide, que es la unica clase de garantia que aguanta el paso del tiempo.</p>
 *
 * <p>Cada endpoint se somete a tres pruebas: sin credenciales debe responder
 * <strong>401</strong>, con un usuario normal <strong>403</strong>, y ademas su
 * codigo debe declarar {@code hasRole('ADMIN')} en el metodo o en la clase.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Blindaje del panel de administracion")
class BlindajeDelPanelTest extends BasePostgis {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redis;

    /**
     * El mapa de handlers de la aplicacion.
     *
     * <p>Hace falta el {@code @Qualifier}: Actuator registra el suyo propio
     * ({@code controllerEndpointHandlerMapping}) y sin nombrar cual se quiere,
     * el contexto no arranca. El bueno es el de siempre.</p>
     */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeoDeRutas;

    private String tokenIntruso;

    @BeforeEach
    void preparar() throws Exception {
        limpiar("rate:*");
        limpiar("antispam:*");

        jdbc.execute("DELETE FROM registro_actividad");
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");

        tokenIntruso = registrar("intruso@yachay.pe");
    }

    // ---------------------------------------------------------------
    //  La prueba de intrusion, endpoint por endpoint
    // ---------------------------------------------------------------

    /**
     * Ataca cada endpoint de {@code /admin} con un usuario autenticado sin rol.
     *
     * <p>Un {@code @TestFactory} y no un {@code @Test} con un bucle: asi cada
     * endpoint aparece con su nombre en el informe, y si uno falla se ve
     * exactamente cual sin tener que leer una traza.</p>
     */
    @TestFactory
    @DisplayName("Un usuario normal recibe 403 en CADA endpoint de admin")
    List<DynamicTest> ningunEndpointSeLeEscapaAUnIntruso() {
        List<EndpointAdmin> endpoints = endpointsDeAdmin();

        assertThat(endpoints)
                .as("si esta lista se vacia, el test dejaria de probar nada")
                .isNotEmpty();

        return endpoints.stream()
                .map(endpoint -> DynamicTest.dynamicTest(
                        endpoint.metodo() + " " + endpoint.ruta(),
                        () -> {
                            MvcResult resultado = mockMvc.perform(peticion(endpoint)
                                            .header("Authorization", "Bearer " + tokenIntruso))
                                    .andReturn();

                            assertThat(resultado.getResponse().getStatus())
                                    .as("%s %s deja pasar a un usuario sin rol ADMIN",
                                            endpoint.metodo(), endpoint.ruta())
                                    .isEqualTo(403);
                        }))
                .toList();
    }

    @TestFactory
    @DisplayName("Sin credenciales, CADA endpoint de admin responde 401")
    List<DynamicTest> sinCredencialesTampoco() {
        return endpointsDeAdmin().stream()
                .map(endpoint -> DynamicTest.dynamicTest(
                        endpoint.metodo() + " " + endpoint.ruta(),
                        () -> {
                            MvcResult resultado = mockMvc.perform(peticion(endpoint)).andReturn();

                            assertThat(resultado.getResponse().getStatus())
                                    .as("%s %s responde a una peticion anonima",
                                            endpoint.metodo(), endpoint.ruta())
                                    .isEqualTo(401);
                        }))
                .toList();
    }

    /**
     * Segunda linea: que el codigo tambien lo declare.
     *
     * <p>Las dos pruebas anteriores comprueban el comportamiento, que es lo que
     * importa. Esta comprueba la <em>intencion</em>: que la restriccion viaje
     * con el codigo y no dependa solo de la regla por URL de
     * {@code SecurityConfig}. Si manana alguien cambia esa regla, los
     * {@code @PreAuthorize} siguen ahi.</p>
     */
    @TestFactory
    @DisplayName("CADA endpoint de admin declara hasRole('ADMIN') en su codigo")
    List<DynamicTest> todosDeclaranSuRestriccion() {
        return endpointsDeAdmin().stream()
                .map(endpoint -> DynamicTest.dynamicTest(
                        endpoint.metodo() + " " + endpoint.ruta(),
                        () -> assertThat(endpoint.declaraRolAdmin())
                                .as("%s %s (%s) no declara @PreAuthorize con ADMIN",
                                        endpoint.metodo(), endpoint.ruta(), endpoint.firma())
                                .isTrue()))
                .toList();
    }

    // ---------------------------------------------------------------
    //  Lo que ya funcionaba y no debe romperse
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("El administrador si entra")
    class ConRolAdmin {

        @Test
        @DisplayName("el dashboard responde 200")
        void dashboardAccesible() throws Exception {
            mockMvc.perform(request(HttpMethod.GET, "/admin/dashboard")
                            .header("Authorization", "Bearer " + crearAdmin()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("un token manipulado no vale ni siendo admin")
        void tokenManipulado() throws Exception {
            String token = crearAdmin();
            // Se altera el PRIMER caracter de la firma: cambiar el ultimo puede
            // no alterar ni un bit significativo en base64url.
            int corte = token.lastIndexOf('.') + 1;
            char original = token.charAt(corte);
            String manipulado = token.substring(0, corte)
                    + (original == 'A' ? 'B' : 'A')
                    + token.substring(corte + 1);

            mockMvc.perform(request(HttpMethod.GET, "/admin/dashboard")
                            .header("Authorization", "Bearer " + manipulado))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ---------------------------------------------------------------
    //  Enumeracion de los endpoints
    // ---------------------------------------------------------------

    /**
     * Todos los handlers registrados cuya ruta empieza por {@code /admin}.
     *
     * <p>Se lee del {@link RequestMappingHandlerMapping}, que es el mismo mapa
     * que usa Spring para despachar las peticiones: si un endpoint existe, esta
     * aqui. No hay forma de anadir uno y que este test no lo vea.</p>
     */
    private List<EndpointAdmin> endpointsDeAdmin() {
        List<EndpointAdmin> encontrados = new ArrayList<>();

        for (var entrada : mapeoDeRutas.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entrada.getKey();
            HandlerMethod handler = entrada.getValue();

            Set<String> patrones = info.getPathPatternsCondition() == null
                    ? Set.of()
                    : info.getPathPatternsCondition().getPatternValues();

            for (String patron : patrones) {
                if (!patron.startsWith("/admin")) {
                    continue;
                }
                Set<org.springframework.web.bind.annotation.RequestMethod> metodos =
                        info.getMethodsCondition().getMethods();

                // Un mapeo sin verbo responde a todos; se prueba con GET.
                String verbo = metodos.isEmpty() ? "GET" : metodos.iterator().next().name();
                encontrados.add(new EndpointAdmin(verbo, patron, handler));
            }
        }

        encontrados.sort(Comparator.comparing(EndpointAdmin::ruta).thenComparing(EndpointAdmin::metodo));
        return encontrados;
    }

    /** Un endpoint del panel, con lo necesario para atacarlo e inspeccionarlo. */
    private record EndpointAdmin(String metodo, String ruta, HandlerMethod handler) {

        /** Ruta con los {@code {id}} sustituidos por un UUID cualquiera. */
        String rutaConcreta() {
            return ruta.replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
        }

        String firma() {
            return handler.getBeanType().getSimpleName() + "#" + handler.getMethod().getName();
        }

        /**
         * ¿El metodo o su clase exigen ADMIN?
         *
         * <p>Se acepta cualquier expresion que nombre el rol: tanto
         * {@code hasRole('ADMIN')} como {@code hasAuthority('ROLE_ADMIN')}.</p>
         */
        boolean declaraRolAdmin() {
            PreAuthorize enMetodo = handler.getMethodAnnotation(PreAuthorize.class);
            PreAuthorize enClase = handler.getBeanType().getAnnotation(PreAuthorize.class);

            return nombraAdmin(enMetodo) || nombraAdmin(enClase);
        }

        private boolean nombraAdmin(PreAuthorize anotacion) {
            return anotacion != null && anotacion.value().contains("ADMIN");
        }
    }

    /**
     * Construye la peticion de ataque.
     *
     * <p>Los verbos con cuerpo llevan un JSON vacio: sin el, Spring podria
     * responder 415 antes de llegar a comprobar los permisos y el test estaria
     * midiendo otra cosa. Con cuerpo, la autorizacion se evalua primero y el
     * resultado esperado es un 403 limpio.</p>
     */
    private MockHttpServletRequestBuilder peticion(EndpointAdmin endpoint) {
        MockHttpServletRequestBuilder constructor =
                request(HttpMethod.valueOf(endpoint.metodo()), endpoint.rutaConcreta());

        if (Set.of("POST", "PUT", "PATCH").contains(endpoint.metodo())) {
            constructor.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return constructor;
    }

    // ---------------------------------------------------------------
    //  Ayudas
    // ---------------------------------------------------------------

    private String registrar(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Yachay2026Dev", "nombre": "Persona"}
                                """.formatted(email)))
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

    private String crearAdmin() throws Exception {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE email = 'jefa@yachay.pe'",
                Integer.class) == 0) {
            registrar("jefa@yachay.pe");
            jdbc.update("""
                    UPDATE usuario SET rol_id = (SELECT id FROM rol WHERE nombre='ADMIN')
                    WHERE email = 'jefa@yachay.pe'
                    """);
        }
        return iniciarSesion("jefa@yachay.pe");
    }

    private void limpiar(String patron) {
        var claves = redis.keys(patron);
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
    }
}
