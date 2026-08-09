package com.huamanga.tourism.common.tiempo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * El unico sitio donde vive la zona horaria de Ayacucho.
 *
 * <p><strong>Por que existe esta clase.</strong> La JVM corre en UTC a
 * proposito (ver {@code TourismApplication}) y el {@code Clock} inyectado es
 * {@code systemUTC()}. Eso significa que {@code LocalDate.now(clock)} devuelve
 * <em>la fecha en UTC</em>, que entre las 19:00 y la medianoche de Ayacucho
 * <strong>ya es el dia siguiente</strong>.</p>
 *
 * <p>Es un error silencioso y desagradable: al caer la tarde, un evento de hoy
 * desapareceria de "proximos eventos" y la cuenta regresiva restaria un dia de
 * mas. Solo se manifiesta en cinco horas de cada dia, asi que es de los que
 * pasan los tests de la manana y fallan en la demostracion de la noche.</p>
 *
 * <p>La regla del proyecto, heredada del desfase horario del Bloque 4: nada
 * depende de la zona por defecto de la maquina, y todo calculo sobre "que dia
 * es hoy en Ayacucho" nombra su zona explicitamente. Este es ese sitio.</p>
 */
public final class TiempoAyacucho {

    /** Ayacucho no cambia de hora, pero fijar la zona lo hace explicito. */
    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private TiempoAyacucho() {
    }

    /**
     * Que dia es hoy <em>en Ayacucho</em>, no en UTC ni en la maquina.
     *
     * <p>Usese siempre en lugar de {@code LocalDate.now(clock)}.</p>
     */
    public static LocalDate hoy(Clock clock) {
        return LocalDate.now(clock.withZone(ZONA));
    }
}
