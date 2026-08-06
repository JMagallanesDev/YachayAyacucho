package com.huamanga.tourism.clima;

import com.huamanga.tourism.clima.config.PropiedadesClima;
import com.huamanga.tourism.clima.dto.ClimaResponse;
import com.huamanga.tourism.clima.service.CacheClima;
import com.huamanga.tourism.clima.service.ClienteOpenWeather;
import com.huamanga.tourism.clima.service.ClimaService;
import com.huamanga.tourism.clima.service.ConsejosClima;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Degradacion elegante del clima (RF-25).
 *
 * <p>Es la parte del bloque que mas facil seria dar por buena sin comprobar:
 * con el proveedor funcionando todo se ve bien, y el comportamiento que
 * importa solo aparece cuando el servicio externo cae. Estos tests fuerzan esa
 * caida.</p>
 *
 * <p>Se usa una cache de mentira en memoria en vez de Redis: lo que se prueba
 * es la <strong>logica de decision</strong> —fresco, ultimo bueno o nada—, no
 * la conexion a Redis, que ya cubren los tests de infraestructura.</p>
 */
@DisplayName("Clima con el proveedor caido")
class ClimaDegradadoTest {

    /** 15:00 en Ayacucho, dentro del horario de visita. */
    private static final Instant AHORA = Instant.parse("2026-08-05T20:00:00Z");

    private ClienteOpenWeather cliente;
    private CacheEnMemoria cache;
    private ClimaService servicio;

    @BeforeEach
    void preparar() {
        cliente = mock(ClienteOpenWeather.class);
        cache = new CacheEnMemoria();
        servicio = new ClimaService(cliente, cache, new ConsejosClima(),
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private ClienteOpenWeather.RespuestaClimaActual respuestaDe(String condicion, double temperatura) {
        return new ClienteOpenWeather.RespuestaClimaActual(
                new ClienteOpenWeather.RespuestaClimaActual.Main(temperatura, temperatura - 1, 60),
                new ClienteOpenWeather.RespuestaClimaActual.Wind(3.0),
                List.of(new ClienteOpenWeather.RespuestaClimaActual.Weather(condicion, "10d", "lluvia")),
                AHORA.getEpochSecond());
    }

    @Nested
    @DisplayName("Con el proveedor respondiendo")
    class Normal {

        @Test
        @DisplayName("devuelve el clima fresco y lo deja cacheado dos veces")
        void guardaEnLasDosClaves() {
            when(cliente.climaActual()).thenReturn(Optional.of(respuestaDe("Rain", 14.0)));

            ClimaResponse clima = servicio.actual();

            assertThat(clima.disponible()).isTrue();
            assertThat(clima.obsoleto()).isFalse();
            assertThat(clima.temperatura()).isEqualTo(14.0);
            assertThat(clima.condicion()).isEqualTo("Rain");

            // La clave del ultimo bueno es la que salva la caida siguiente.
            assertThat(cache.contenido).containsKeys("clima:actual:fresco", "clima:actual:ultimo-bueno");
        }

        @Test
        @DisplayName("no vuelve a llamar al proveedor mientras el dato siga fresco")
        void reusaElFresco() {
            when(cliente.climaActual()).thenReturn(Optional.of(respuestaDe("Clear", 20.0)));

            servicio.actual();
            servicio.actual();
            servicio.actual();

            assertThat(cache.lecturasDelProveedor)
                    .as("tres visitas deben costar una sola llamada externa")
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Con el proveedor caido")
    class Caido {

        @Test
        @DisplayName("sirve el ultimo clima conocido, marcado como obsoleto")
        void sirveElUltimoBueno() {
            // Primero una llamada correcta que deja el rastro...
            when(cliente.climaActual()).thenReturn(Optional.of(respuestaDe("Rain", 14.0)));
            servicio.actual();

            // ...luego caduca lo fresco y el proveedor deja de responder.
            cache.caducarFresco();
            when(cliente.climaActual()).thenReturn(Optional.empty());

            ClimaResponse clima = servicio.actual();

            assertThat(clima.disponible())
                    .as("hay dato: es viejo, pero existe")
                    .isTrue();
            assertThat(clima.obsoleto())
                    .as("y se declara viejo en vez de disfrazarse de actual")
                    .isTrue();
            assertThat(clima.temperatura()).isEqualTo(14.0);
            assertThat(clima.medidoEn())
                    .as("con su marca de tiempo, para poder decir 'hace 2 h'")
                    .isNotNull();
        }

        @Test
        @DisplayName("sin nada cacheado responde 'no disponible' en vez de romperse")
        void sinNadaCacheado() {
            when(cliente.climaActual()).thenReturn(Optional.empty());

            ClimaResponse clima = servicio.actual();

            assertThat(clima.disponible()).isFalse();
            assertThat(clima.temperatura()).isNull();
            // Lo importante: no se lanzo ninguna excepcion. El clima es
            // informacion accesoria y su ausencia no puede tumbar la pagina.
        }

        @Test
        @DisplayName("el dato obsoleto sobrevive a varias consultas seguidas")
        void obsoletoEstable() {
            when(cliente.climaActual()).thenReturn(Optional.of(respuestaDe("Clouds", 16.0)));
            servicio.actual();
            cache.caducarFresco();
            when(cliente.climaActual()).thenReturn(Optional.empty());

            assertThat(servicio.actual().obsoleto()).isTrue();
            assertThat(servicio.actual().obsoleto()).isTrue();
            assertThat(servicio.actual().temperatura()).isEqualTo(16.0);
        }
    }

    @Nested
    @DisplayName("Consejos (RF-27)")
    class Consejos {

        @Test
        @DisplayName("con lluvia aconseja impermeable")
        void lluvia() {
            when(cliente.climaActual()).thenReturn(Optional.of(respuestaDe("Rain", 14.0)));
            assertThat(servicio.actual().consejos()).contains("lluvia");
        }

        @Test
        @DisplayName("con cielo despejado a media tarde aconseja proteccion solar")
        void sol() {
            when(cliente.climaActual()).thenReturn(Optional.of(respuestaDe("Clear", 21.0)));

            List<String> consejos = servicio.actual().consejos();

            assertThat(consejos).contains("protectorSolar");
            // A las 15:00 de Ayacucho tambien toca el aviso de radiacion de
            // altura y el del atardecer aun no.
            assertThat(consejos).contains("uvAlto");
        }
    }

    /**
     * Cache de mentira con la misma logica de decision que la real.
     *
     * <p>Extiende {@link CacheClima} y reemplaza su almacen por un mapa. Asi el
     * test ejercita el orden fresco -> proveedor -> ultimo bueno -> vacio sin
     * levantar Redis.</p>
     */
    private static class CacheEnMemoria extends CacheClima {

        final Map<String, Object> contenido = new HashMap<>();
        int lecturasDelProveedor = 0;

        CacheEnMemoria() {
            super(null, new PropiedadesClima("clave", null, 0, 0,
                    Duration.ofMinutes(30), Duration.ofHours(24), Duration.ofSeconds(3)));
        }

        @Override
        public <T> T resolver(String nombre, Class<T> tipo,
                              java.util.function.Supplier<Optional<T>> proveedor,
                              java.util.function.UnaryOperator<T> obsoleto,
                              java.util.function.Supplier<T> vacio) {

            Object fresco = contenido.get(nombre + ":fresco");
            if (tipo.isInstance(fresco)) {
                return tipo.cast(fresco);
            }

            lecturasDelProveedor++;
            Optional<T> nuevo = proveedor.get();
            if (nuevo.isPresent()) {
                contenido.put(nombre + ":fresco", nuevo.get());
                contenido.put(nombre + ":ultimo-bueno", nuevo.get());
                return nuevo.get();
            }

            Object ultimo = contenido.get(nombre + ":ultimo-bueno");
            if (tipo.isInstance(ultimo)) {
                return obsoleto.apply(tipo.cast(ultimo));
            }
            return vacio.get();
        }

        /** Simula el paso de los 30 minutos sin esperar 30 minutos. */
        void caducarFresco() {
            contenido.keySet().removeIf(clave -> clave.endsWith(":fresco"));
        }
    }
}
