package com.huamanga.tourism.recomendacion;

import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.dto.LugarDetalleResponse.CategoriaResponse;
import com.huamanga.tourism.lugar.dto.LugarResumenResponse;
import com.huamanga.tourism.recomendacion.service.ReglasRecomendacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reglas de la recomendacion contextual (RF-08).
 *
 * <p>Todo entra por parametro —hora, clima, estado de apertura— asi que no hay
 * nada que dependa del momento en que se ejecute el test. Un motor de reglas
 * que solo se puede comprobar «a ojo» acaba siendo indistinguible de una
 * supersticion.</p>
 */
@DisplayName("Reglas de recomendacion")
class ReglasRecomendacionTest {

    private final ReglasRecomendacion reglas = new ReglasRecomendacion();

    private LugarResumenResponse lugar(String categoria, BigDecimal calificacion, Short duracion) {
        return new LugarResumenResponse(
                UUID.randomUUID(), "un-lugar", "Un lugar", "Descripcion",
                new CategoriaResponse(UUID.randomUUID(), categoria, categoria, "icono", "#000000"),
                -74.22, -13.15,
                BigDecimal.TEN, duracion, EstadoLugar.PUBLICADO,
                List.of(), calificacion, 0L, 0L);
    }

    private LugarResumenResponse lugar(String categoria) {
        return lugar(categoria, BigDecimal.ZERO, (short) 60);
    }

    @Nested
    @DisplayName("Abierto o cerrado")
    class Apertura {

        @Test
        @DisplayName("un lugar cerrado queda descartado, no solo penalizado")
        void cerradoSeDescarta() {
            var resultado = reglas.evaluar(lugar("MUSEOS"), false, "Rain",
                    LocalTime.of(15, 0), true);

            assertThat(resultado.descartado())
                    .as("mandar a alguien a una puerta cerrada es el peor resultado posible")
                    .isTrue();
        }

        @Test
        @DisplayName("estar abierto es el primer motivo que se muestra")
        void abiertoEsMotivo() {
            var resultado = reglas.evaluar(lugar("MUSEOS"), true, null,
                    LocalTime.of(15, 0), false);

            assertThat(resultado.descartado()).isFalse();
            assertThat(resultado.motivos()).contains("abiertoAhora");
        }
    }

    @Nested
    @DisplayName("Clima")
    class Clima {

        @Test
        @DisplayName("si llueve, un museo puntua por encima de un mirador")
        void lluviaPrefiereTecho() {
            var museo = reglas.evaluar(lugar("MUSEOS"), true, "Rain", LocalTime.of(15, 0), true);
            var mirador = reglas.evaluar(lugar("MIRADORES"), true, "Rain", LocalTime.of(15, 0), true);

            assertThat(museo.puntuacion()).isGreaterThan(mirador.puntuacion());
            assertThat(museo.motivos()).contains("aCubierto");
        }

        @Test
        @DisplayName("con cielo despejado, el mirador adelanta al museo")
        void solPrefiereAireLibre() {
            var mirador = reglas.evaluar(lugar("MIRADORES"), true, "Clear", LocalTime.of(15, 0), true);
            var museo = reglas.evaluar(lugar("MUSEOS"), true, "Clear", LocalTime.of(15, 0), true);

            assertThat(mirador.puntuacion()).isGreaterThan(museo.puntuacion());
            assertThat(mirador.motivos()).contains("cieloDespejado");
        }

        @Test
        @DisplayName("si el clima no es fiable, no se puntua por el")
        void climaNoFiableNoInfluye() {
            var conClima = reglas.evaluar(lugar("MUSEOS"), true, "Rain", LocalTime.of(15, 0), false);
            var sinClima = reglas.evaluar(lugar("MUSEOS"), true, null, LocalTime.of(15, 0), false);

            assertThat(conClima.puntuacion())
                    .as("un clima obsoleto no debe decidir a donde va nadie")
                    .isEqualTo(sinClima.puntuacion());
            assertThat(conClima.motivos()).doesNotContain("aCubierto");
        }

        @Test
        @DisplayName("sin clima disponible sigue recomendando")
        void sinClimaSigueFuncionando() {
            var resultado = reglas.evaluar(lugar("MUSEOS"), true, null, LocalTime.of(15, 0), false);

            assertThat(resultado.descartado()).isFalse();
            assertThat(resultado.puntuacion()).isPositive();
        }
    }

    @Nested
    @DisplayName("Hora del dia")
    class Hora {

        @Test
        @DisplayName("el mirador gana peso al atardecer")
        void miradorAlAtardecer() {
            var tarde = reglas.evaluar(lugar("MIRADORES"), true, null, LocalTime.of(17, 30), false);
            var manana = reglas.evaluar(lugar("MIRADORES"), true, null, LocalTime.of(9, 0), false);

            assertThat(tarde.puntuacion()).isGreaterThan(manana.puntuacion());
            assertThat(tarde.motivos()).contains("horaDelAtardecer");
        }

        @Test
        @DisplayName("los talleres de artesania se recomiendan por la manana")
        void artesaniaPorLaManana() {
            var manana = reglas.evaluar(lugar("ARTESANIA"), true, null, LocalTime.of(9, 0), false);

            assertThat(manana.motivos()).contains("mejorPorLaManana");
        }

        @Test
        @DisplayName("de noche penaliza las visitas largas")
        void deNocheNadaLargo() {
            var corta = reglas.evaluar(lugar("MUSEOS", BigDecimal.ZERO, (short) 45),
                    true, null, LocalTime.of(20, 0), false);
            var larga = reglas.evaluar(lugar("MUSEOS", BigDecimal.ZERO, (short) 180),
                    true, null, LocalTime.of(20, 0), false);

            assertThat(corta.puntuacion()).isGreaterThan(larga.puntuacion());
        }
    }

    @Nested
    @DisplayName("Desempates")
    class Desempates {

        @Test
        @DisplayName("un lugar bien valorado adelanta a uno sin resenas")
        void valoracionDesempata() {
            var bueno = reglas.evaluar(lugar("MUSEOS", new BigDecimal("4.6"), (short) 60),
                    true, null, LocalTime.of(15, 0), false);
            var sinResenas = reglas.evaluar(lugar("MUSEOS", BigDecimal.ZERO, (short) 60),
                    true, null, LocalTime.of(15, 0), false);

            assertThat(bueno.puntuacion()).isGreaterThan(sinResenas.puntuacion());
            assertThat(bueno.motivos()).contains("bienValorado");
        }

        @Test
        @DisplayName("una categoria desconocida no rompe nada")
        void categoriaNuevaNoRompe() {
            var resultado = reglas.evaluar(lugar("CATEGORIA_QUE_NO_EXISTE"), true, "Rain",
                    LocalTime.of(15, 0), true);

            assertThat(resultado.descartado()).isFalse();
            assertThat(resultado.puntuacion()).isPositive();
        }
    }
}
