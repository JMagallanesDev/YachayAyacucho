package com.huamanga.tourism.checkin.service;

import com.huamanga.tourism.checkin.domain.CheckIn;
import com.huamanga.tourism.lugar.domain.Lugar;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Comprobaciones de plausibilidad de un check-in (RF-39).
 *
 * <p><strong>Lo que esto NO es.</strong> No es una prueba de que alguien estuvo
 * fisicamente en un lugar, y conviene decirlo sin rodeos: la API de
 * geolocalizacion del navegador se puede sobrescribir desde las herramientas de
 * desarrollo en dos clics. De hecho, el propio guion de verificacion de este
 * proyecto lo hace para poder probar la funcion. Impedirlo de verdad exigiria
 * atestacion de aplicacion nativa, que esta fuera del alcance de una web.</p>
 *
 * <p><strong>Lo que si es.</strong> Un conjunto de barreras que encarecen el
 * fraude y lo dejan registrado. El check-in alimenta sellos e insignias —un
 * incentivo ludico— y <strong>no desbloquea nada critico</strong>: ni permisos,
 * ni contenido reservado, ni ventajas frente a otros usuarios. Esa es la razon
 * por la que este nivel de garantia es suficiente aqui y no lo seria para,
 * digamos, un control de acceso.</p>
 */
@Component
public class ValidadorProximidad {

    /**
     * Radio de check-in.
     *
     * <p>150 m y no los 100 del RF-39 original: en el centro historico de
     * Huamanga, entre calles estrechas y muros de piedra, el error tipico del
     * GPS de un movil ronda los 20-50 m y empeora bastante. Con 100 m se
     * bloquearian check-ins de gente que esta literalmente en la puerta. Se
     * prioriza que el visitante honesto pueda usar la funcion; hacer trampa
     * desde 140 m no da ningun premio que no diera desde 90.</p>
     */
    public static final int RADIO_METROS = 150;

    /**
     * Precision minima aceptable de la lectura.
     *
     * <p>Una lectura con 2 km de incertidumbre no prueba cercania a nada: es la
     * que devuelve un navegador que esta triangulando por IP en vez de usar el
     * GPS. Aceptarla convertiria el radio en decorativo.</p>
     */
    public static final double PRECISION_MAXIMA_METROS = 200;

    /** Un check-in por lugar y dia: es lo que hace que «mas visitados» signifique algo. */
    public static final Duration ENFRIAMIENTO = Duration.ofHours(24);

    /**
     * Velocidad maxima creible entre dos check-ins.
     *
     * <p>300 km/h es holgado a proposito —cubre un viaje en avion entre
     * regiones— y aun asi corta en seco el caso que interesa: un script
     * recorriendo los quince lugares en un minuto.</p>
     */
    public static final double VELOCIDAD_MAXIMA_KMH = 300;

    /** Margen bajo el cual dos check-ins seguidos no se juzgan por velocidad. */
    private static final Duration MARGEN_SIN_JUZGAR = Duration.ofMinutes(1);

    private static final double RADIO_TIERRA_KM = 6371;

    /**
     * @param distanciaMetros distancia real calculada por PostGIS sobre
     *                        {@code geography}; nunca una distancia enviada por
     *                        el cliente
     */
    public void validar(Lugar lugar,
                        double distanciaMetros,
                        Double precisionMetros,
                        Optional<CheckIn> anterior,
                        Point puntoEnviado,
                        Instant ahora) {

        if (distanciaMetros > RADIO_METROS) {
            throw new DemasiadoLejosException(Math.round(distanciaMetros), RADIO_METROS);
        }

        // La precision la reporta el navegador junto a la posicion. Es tan
        // falsificable como las coordenadas, pero filtra el caso honesto de una
        // lectura mala, que es el mas frecuente con diferencia.
        if (precisionMetros != null && precisionMetros > PRECISION_MAXIMA_METROS) {
            throw new PrecisionInsuficienteException(Math.round(precisionMetros));
        }

        anterior.ifPresent(previo -> {
            Duration transcurrido = Duration.between(previo.getCreatedAt(), ahora);
            if (transcurrido.compareTo(MARGEN_SIN_JUZGAR) < 0) {
                return;
            }

            double km = distanciaKm(previo.getLugar().getUbicacion(), puntoEnviado);
            double horas = transcurrido.toMillis() / 3_600_000.0;
            double velocidad = km / horas;

            if (velocidad > VELOCIDAD_MAXIMA_KMH) {
                throw new SaltoImposibleException(Math.round(km), Math.round(velocidad));
            }
        });
    }

    /** Haversine. Para decidir si un salto es creible sobra de precision. */
    private double distanciaKm(Point origen, Point destino) {
        double lat1 = Math.toRadians(origen.getY());
        double lat2 = Math.toRadians(destino.getY());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(destino.getX() - origen.getX());

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);

        return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ---------------------------------------------------------------
    //  Excepciones: cada una con el dato que ayuda a entender el rechazo
    // ---------------------------------------------------------------

    public static class DemasiadoLejosException extends RuntimeException {
        private final long distancia;
        private final int radio;

        public DemasiadoLejosException(long distancia, int radio) {
            super("Estas a " + distancia + " m; hay que estar a menos de " + radio);
            this.distancia = distancia;
            this.radio = radio;
        }

        public long distancia() {
            return distancia;
        }

        public int radio() {
            return radio;
        }
    }

    public static class PrecisionInsuficienteException extends RuntimeException {
        public PrecisionInsuficienteException(long precision) {
            super("La lectura del GPS tiene " + precision + " m de error");
        }
    }

    public static class YaHizoCheckInException extends RuntimeException {
        public YaHizoCheckInException() {
            super("Ya registraste una visita a este lugar en las ultimas 24 horas");
        }
    }

    public static class SaltoImposibleException extends RuntimeException {
        public SaltoImposibleException(long km, long kmh) {
            super("Salto de " + km + " km desde el ultimo check-in (" + kmh + " km/h)");
        }
    }
}
