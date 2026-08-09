package com.huamanga.tourism.evento;

import com.huamanga.tourism.clima.dto.PronosticoResponse;
import com.huamanga.tourism.clima.service.ClimaService;
import com.huamanga.tourism.common.tiempo.TiempoAyacucho;
import com.huamanga.tourism.evento.domain.Temporada;
import com.huamanga.tourism.evento.dto.ClimaEventoResponse;
import com.huamanga.tourism.evento.service.ClimaEventoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Las dos cosas que mas facil se rompen en una agenda: <strong>en que dia
 * estamos</strong> y <strong>que clima se puede prometer</strong>.
 *
 * <p>Son tests sin contexto de Spring a proposito. Lo que se prueba aqui es
 * aritmetica de calendario y una decision de tres ramas; levantar PostgreSQL
 * para eso solo lo haria mas lento y mas dificil de leer.</p>
 */
@DisplayName("Fechas y clima de los eventos")
class FechasYClimaDeEventosTest {

    // ---------------------------------------------------------------
    //  El desfase horario, que ya mordio una vez en el Bloque 4
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Que dia es hoy en Ayacucho")
    class DiaDeHoy {

        /**
         * Este es el test que impide que vuelva el fallo del Bloque 4, ahora en
         * su version de fechas: la JVM corre en UTC, asi que al caer la tarde en
         * Huamanga el reloj del servidor ya esta en el dia siguiente.
         */
        @Test
        @DisplayName("a las 21:00 de Huamanga sigue siendo HOY, no manana")
        void alAnochecerSigueSiendoHoy() {
            // 02:00 UTC del 7 de agosto son las 21:00 del 6 en Ayacucho.
            Clock reloj = Clock.fixed(Instant.parse("2026-08-07T02:00:00Z"), ZoneOffset.UTC);

            assertThat(TiempoAyacucho.hoy(reloj))
                    .as("una fiesta de hoy no puede desaparecer de la portada a las nueve de la noche")
                    .isEqualTo(LocalDate.of(2026, 8, 6));

            // Y la prueba de que la precaucion no es teorica: lo ingenuo falla.
            assertThat(LocalDate.now(reloj))
                    .as("LocalDate.now(clock) sobre un reloj UTC ya es el dia siguiente")
                    .isEqualTo(LocalDate.of(2026, 8, 7));
        }

        @Test
        @DisplayName("a las 09:00 de Huamanga coincide con UTC")
        void porLaMananaCoinciden() {
            Clock reloj = Clock.fixed(Instant.parse("2026-08-06T14:00:00Z"), ZoneOffset.UTC);
            assertThat(TiempoAyacucho.hoy(reloj)).isEqualTo(LocalDate.of(2026, 8, 6));
        }
    }

    // ---------------------------------------------------------------
    //  Las temporadas, que es lo unico que se puede decir de un evento
    //  lejano sin inventar
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Temporada del ano andino")
    class Temporadas {

        @Test
        @DisplayName("de noviembre a marzo es epoca de lluvias")
        void lluvias() {
            assertThat(Temporada.de(Month.NOVEMBER)).isEqualTo(Temporada.LLUVIAS);
            assertThat(Temporada.de(Month.DECEMBER)).isEqualTo(Temporada.LLUVIAS);
            assertThat(Temporada.de(Month.JANUARY)).isEqualTo(Temporada.LLUVIAS);
            assertThat(Temporada.de(Month.MARCH)).isEqualTo(Temporada.LLUVIAS);
        }

        @Test
        @DisplayName("de abril a octubre es epoca seca")
        void seca() {
            assertThat(Temporada.de(Month.APRIL)).isEqualTo(Temporada.SECA);
            assertThat(Temporada.de(Month.AUGUST)).isEqualTo(Temporada.SECA);
            assertThat(Temporada.de(Month.OCTOBER)).isEqualTo(Temporada.SECA);
        }
    }

    // ---------------------------------------------------------------
    //  Los cuatro estados del clima de un evento (RF-88)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Clima de un evento")
    class ClimaDelEvento {

        private static final Instant AHORA = Instant.parse("2026-08-06T14:00:00Z");
        private static final LocalDate HOY = LocalDate.of(2026, 8, 6);

        private ClimaEventoService servicioCon(PronosticoResponse pronostico) {
            ClimaService clima = mock(ClimaService.class);
            when(clima.pronostico()).thenReturn(pronostico);
            return new ClimaEventoService(clima, Clock.fixed(AHORA, ZoneOffset.UTC));
        }

        private PronosticoResponse pronosticoDe(int dias) {
            List<PronosticoResponse.PronosticoDia> lista = java.util.stream.IntStream.range(0, dias)
                    .mapToObj(i -> new PronosticoResponse.PronosticoDia(
                            HOY.plusDays(i), 8.0, 19.0, "Clouds", "03d", 0.2, List.of()))
                    .toList();
            return new PronosticoResponse(lista, AHORA, true, false);
        }

        @Test
        @DisplayName("dentro de la ventana, devuelve el pronostico real")
        void dentroDeLaVentana() {
            var clima = servicioCon(pronosticoDe(5)).paraEvento(HOY.plusDays(3), HOY.plusDays(3));

            assertThat(clima.estado()).isEqualTo(ClimaEventoResponse.Estado.PRONOSTICO);
            assertThat(clima.dia()).isNotNull();
            assertThat(clima.dia().maxima()).isEqualTo(19.0);
            assertThat(clima.diasParaElEvento()).isEqualTo(3);
        }

        /**
         * El caso normal de una agenda cultural, y el que no puede parecerse a
         * un error: casi todas las festividades estan a meses vista.
         */
        @Test
        @DisplayName("mas alla de la ventana NO es un error: informa de la temporada")
        void masAllaDeLaVentana() {
            var clima = servicioCon(pronosticoDe(5)).paraEvento(
                    LocalDate.of(2026, 12, 9), LocalDate.of(2026, 12, 9));

            assertThat(clima.estado()).isEqualTo(ClimaEventoResponse.Estado.FUERA_DE_ALCANCE);
            assertThat(clima.dia()).as("no se inventa un pronostico").isNull();
            assertThat(clima.temporada())
                    .as("diciembre en la sierra es epoca de lluvias, y eso si es cierto")
                    .isEqualTo(Temporada.LLUVIAS);
            assertThat(clima.diasParaElEvento()).isEqualTo(125);
        }

        @Test
        @DisplayName("si el proveedor esta caido, se dice que no esta disponible")
        void proveedorCaido() {
            var clima = servicioCon(PronosticoResponse.noDisponible())
                    .paraEvento(HOY.plusDays(2), HOY.plusDays(2));

            assertThat(clima.estado()).isEqualTo(ClimaEventoResponse.Estado.NO_DISPONIBLE);
            assertThat(clima.temporada())
                    .as("aunque el proveedor falle, la temporada se sabe igual")
                    .isEqualTo(Temporada.SECA);
        }

        @Test
        @DisplayName("un evento que ya paso no muestra clima")
        void eventoPasado() {
            var clima = servicioCon(pronosticoDe(5)).paraEvento(HOY.minusDays(10), HOY.minusDays(8));

            assertThat(clima.estado()).isEqualTo(ClimaEventoResponse.Estado.PASADO);
            assertThat(clima.dia()).isNull();
            assertThat(clima.temporada()).isNull();
        }

        @Test
        @DisplayName("de un evento EN CURSO se da el clima de hoy, no el del dia que empezo")
        void eventoEnCurso() {
            var clima = servicioCon(pronosticoDe(5)).paraEvento(HOY.minusDays(2), HOY.plusDays(2));

            assertThat(clima.estado()).isEqualTo(ClimaEventoResponse.Estado.PRONOSTICO);
            assertThat(clima.dia().fecha())
                    .as("quien lo consulta esta decidiendo si acercarse ahora")
                    .isEqualTo(HOY);
            assertThat(clima.diasParaElEvento()).isZero();
        }

        @Test
        @DisplayName("un viaje largo mezcla pronostico y temporada sin sobresaltos")
        void viajeLargo() {
            List<LocalDate> viaje = HOY.datesUntil(HOY.plusDays(10)).toList();
            var climas = servicioCon(pronosticoDe(5)).paraDias(viaje);

            assertThat(climas).hasSize(10);
            assertThat(climas.subList(0, 5))
                    .allMatch(c -> c.estado() == ClimaEventoResponse.Estado.PRONOSTICO);
            assertThat(climas.subList(5, 10))
                    .as("los dias lejanos degradan a temporada, no a error")
                    .allMatch(c -> c.estado() == ClimaEventoResponse.Estado.FUERA_DE_ALCANCE);
        }
    }
}
