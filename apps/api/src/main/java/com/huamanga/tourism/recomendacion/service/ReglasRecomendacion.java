package com.huamanga.tourism.recomendacion.service;

import com.huamanga.tourism.lugar.dto.LugarResumenResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de la recomendacion contextual (RF-08).
 *
 * <p>Separadas del servicio a proposito: sin estado ni dependencias, se pueden
 * probar con datos fijos. Un motor de reglas que no se puede verificar deja de
 * ser un motor y pasa a ser una supersticion.</p>
 *
 * <p>Las categorias se identifican por su <strong>codigo</strong>
 * ({@code MUSEOS}, {@code MIRADORES}...), nunca por el nombre traducido: el
 * nombre cambia con el idioma y las reglas dejarian de aplicarse en ingles.</p>
 */
@Component
public class ReglasRecomendacion {

    /** Puntuacion base para que un lugar sin ninguna ventaja siga apareciendo. */
    private static final int BASE = 10;

    // Los codigos son los ocho de la migracion V14: IGLESIAS, MUSEOS,
    // MIRADORES, SITIOS_ARQUEOLOGICOS, PLAZAS, ARTESANIA, GASTRONOMIA y
    // NATURALEZA. Si algun dia se anade una categoria, cae con la puntuacion
    // base y no rompe nada.
    private static final List<String> A_CUBIERTO =
            List.of("MUSEOS", "IGLESIAS", "ARTESANIA", "GASTRONOMIA");
    private static final List<String> AL_AIRE_LIBRE =
            List.of("MIRADORES", "NATURALEZA", "PLAZAS", "SITIOS_ARQUEOLOGICOS");

    /**
     * @param abierto     si el lugar admite visitantes ahora mismo
     * @param condicion   condicion meteorologica en codigo, o null si no se sabe
     * @param hora        hora local de Ayacucho
     * @param climaFiable false cuando el clima no esta disponible o es obsoleto,
     *                    en cuyo caso no se puntua por el
     */
    public Resultado evaluar(LugarResumenResponse lugar, boolean abierto, String condicion,
                             LocalTime hora, boolean climaFiable) {

        List<String> motivos = new ArrayList<>();
        int puntos = BASE;

        // ---- Abierto: no es una preferencia, es un requisito -------------
        // Recomendar «ahora» algo cerrado manda a alguien a una puerta
        // cerrada, que es el peor resultado posible de este motor.
        if (!abierto) {
            return new Resultado(Integer.MIN_VALUE, List.of());
        }
        puntos += 20;
        motivos.add("abiertoAhora");

        String categoria = lugar.categoria() != null ? lugar.categoria().codigo() : null;

        // ---- Clima -------------------------------------------------------
        if (climaFiable && categoria != null) {
            boolean llueve = condicion != null
                    && (condicion.equalsIgnoreCase("Rain")
                    || condicion.equalsIgnoreCase("Drizzle")
                    || condicion.equalsIgnoreCase("Thunderstorm"));

            if (llueve && A_CUBIERTO.contains(categoria)) {
                puntos += 25;
                motivos.add("aCubierto");
            } else if (llueve && AL_AIRE_LIBRE.contains(categoria)) {
                puntos -= 30;
            } else if ("Clear".equalsIgnoreCase(condicion) && AL_AIRE_LIBRE.contains(categoria)) {
                puntos += 20;
                motivos.add("cieloDespejado");
            }
        }

        // ---- Hora del dia ------------------------------------------------
        if (categoria != null && hora != null) {
            int h = hora.getHour();

            // Los talleres de artesania trabajan de manana; por la tarde se
            // visita el escaparate, no el oficio.
            if (h >= 8 && h < 12 && "ARTESANIA".equals(categoria)) {
                puntos += 15;
                motivos.add("mejorPorLaManana");
            }
            // A 2 761 m el sol de mediodia sobre una ruina sin sombra es duro.
            if (h >= 11 && h < 15 && "SITIOS_ARQUEOLOGICOS".equals(categoria)) {
                puntos -= 10;
            }
            if (h >= 16 && h < 19 && "MIRADORES".equals(categoria)) {
                puntos += 20;
                motivos.add("horaDelAtardecer");
            }
            if ("GASTRONOMIA".equals(categoria) && ((h >= 12 && h < 15) || (h >= 19 && h < 22))) {
                puntos += 15;
                motivos.add("horaDeComer");
            }
            // De noche, lo lejano deja de ser buena idea aunque este abierto.
            if (h >= 19 && lugar.duracionVisitaMin() != null && lugar.duracionVisitaMin() > 120) {
                puntos -= 15;
            }
        }

        // ---- Valoracion: solo desempata ----------------------------------
        BigDecimal calificacion = lugar.calificacionPromedio();
        if (calificacion != null && calificacion.compareTo(new BigDecimal("4.0")) >= 0) {
            puntos += 10;
            motivos.add("bienValorado");
        }

        // ---- Entrada libre ------------------------------------------------
        if (lugar.precioEntradaPen() != null
                && lugar.precioEntradaPen().compareTo(BigDecimal.ZERO) == 0) {
            puntos += 5;
            motivos.add("entradaLibre");
        }

        return new Resultado(puntos, List.copyOf(motivos));
    }

    public record Resultado(int puntuacion, List<String> motivos) {

        /** Un lugar cerrado queda descartado, no simplemente al final. */
        public boolean descartado() {
            return puntuacion == Integer.MIN_VALUE;
        }
    }
}
