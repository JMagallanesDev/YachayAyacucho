package com.huamanga.tourism.analitica.service;

import com.huamanga.tourism.analitica.domain.TipoPagina;
import com.huamanga.tourism.common.seguridad.HuellaAnonima;
import com.huamanga.tourism.common.tiempo.TiempoAyacucho;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Registro de visitas agregadas (RF-52b).
 *
 * <p><strong>Que NO se guarda, y como se consigue el dato igualmente.</strong>
 * No hay IP, ni {@code usuario_id}, ni user-agent, ni sesion, ni una tabla de
 * eventos crudos: solo dos enteros por seccion y dia. La coherencia con el
 * anonimato del Bloque 8 no es una promesa operativa, es que el dato personal no
 * llega a existir en ningun sitio duradero.</p>
 *
 * <p><strong>Las visitas unicas salen de un HyperLogLog.</strong> Es la pieza
 * que hace posible contar personas distintas sin saber quienes son: un HLL no
 * almacena los elementos que se le anaden, sino un boceto probabilistico de unos
 * 12 KB del que <em>no se puede extraer ni enumerar</em> a nadie —solo preguntar
 * cuantos hubo, con un error de ~0,8 %—. Y vive en Redis con caducidad, asi que
 * se autodestruye.</p>
 *
 * <p><strong>El throttling evita inflar el contador.</strong> Una ventana de 30
 * minutos por huella y seccion: recargar la pagina, navegar ida y vuelta o dejar
 * la pestana abierta no suma visitas nuevas. Ademas reduce la escritura a la
 * base a una por visitante y media hora, que es lo que permite escribir directo
 * sin acumular en memoria.</p>
 */
@Service
public class RegistroVisitasService {

    private static final Logger log = LoggerFactory.getLogger(RegistroVisitasService.class);

    /** Ventana durante la cual una misma huella no vuelve a contar. */
    private static final Duration VENTANA_ANTI_RECARGA = Duration.ofMinutes(30);

    /** El boceto de unicas se guarda un dia mas, por si el volcado se retrasa. */
    private static final Duration VIDA_DEL_BOCETO = Duration.ofHours(48);

    private final StringRedisTemplate redis;
    private final HuellaAnonima huellas;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public RegistroVisitasService(StringRedisTemplate redis, HuellaAnonima huellas,
                                  JdbcTemplate jdbc, Clock clock) {
        this.redis = redis;
        this.huellas = huellas;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * Anota una visita a una seccion, si no es una recarga.
     *
     * @return true si la visita se conto; false si cayo dentro de la ventana
     */
    @Transactional
    public boolean registrar(TipoPagina tipo, HttpServletRequest peticion) {
        LocalDate hoy = TiempoAyacucho.hoy(clock);
        String huella = huellas.de(peticion);

        if (!esVisitaNueva(tipo, huella, hoy)) {
            return false;
        }

        long unicas = marcarComoUnica(tipo, huella, hoy);
        acumular(tipo, hoy, unicas);
        return true;
    }

    /**
     * Marca la ventana anti-recarga. Devuelve false si ya estaba marcada.
     *
     * <p>Se usa {@code SET NX} y no un {@code GET} seguido de {@code SET}: dos
     * pestanas abiertas a la vez son dos peticiones concurrentes, y con dos
     * operaciones separadas ambas verian la clave vacia y ambas contarian.</p>
     *
     * <p>Si Redis no responde <strong>no se cuenta la visita</strong>. Es la
     * decision contraria a la del anti-spam del Bloque 8, y a proposito: alli
     * fallar cerrando habria silenciado una denuncia; aqui fallar abriendo
     * ensuciaria las metricas sin ningun freno. Perder unas visitas mientras la
     * cache esta caida es el error barato.</p>
     */
    private boolean esVisitaNueva(TipoPagina tipo, String huella, LocalDate dia) {
        String clave = "visita:vista:" + dia + ":" + tipo + ":" + huella;
        try {
            Boolean primera = redis.opsForValue().setIfAbsent(clave, "1", VENTANA_ANTI_RECARGA);
            return Boolean.TRUE.equals(primera);
        } catch (Exception e) {
            log.warn("Sin Redis no se cuentan visitas, para no falsear las metricas: {}",
                    e.getMessage());
            return false;
        }
    }

    /** Anade la huella al boceto del dia y devuelve cuantas distintas van. */
    private long marcarComoUnica(TipoPagina tipo, String huella, LocalDate dia) {
        String clave = "visita:unicas:" + dia + ":" + tipo;
        try {
            redis.opsForHyperLogLog().add(clave, huella);
            redis.expire(clave, VIDA_DEL_BOCETO);
            return redis.opsForHyperLogLog().size(clave);
        } catch (Exception e) {
            log.warn("No se pudo actualizar el boceto de unicas: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Suma la visita a la fila del dia.
     *
     * <p>Un unico enunciado atomico sobre el {@code UNIQUE (tipo_pagina, fecha)}
     * que ya trae la tabla. Sin {@code SELECT} previo: con varias instancias del
     * backend, leer-y-despues-escribir perderia visitas cada vez que dos
     * peticiones coincidieran.</p>
     *
     * <p>{@code visitas_unicas} se <strong>asigna</strong>, no se incrementa: el
     * valor bueno es el que sabe el HyperLogLog, no la suma de incrementos. El
     * {@code GREATEST} lo protege de retroceder si el boceto se perdiera.</p>
     */
    private void acumular(TipoPagina tipo, LocalDate dia, long unicas) {
        jdbc.update("""
                INSERT INTO visita_resumen_diario (id, tipo_pagina, fecha, total_visitas, visitas_unicas)
                VALUES (?, ?, ?, 1, ?)
                ON CONFLICT (tipo_pagina, fecha) DO UPDATE
                SET total_visitas  = visita_resumen_diario.total_visitas + 1,
                    visitas_unicas = GREATEST(visita_resumen_diario.visitas_unicas, EXCLUDED.visitas_unicas),
                    updated_at     = NOW()
                """, UUID.randomUUID(), tipo.name(), dia, (int) unicas);
    }

    /**
     * Igual que lo anterior, para la ficha de un negocio (RF-52b).
     *
     * <p>Queda operativo desde ya, pero <strong>no tendra datos hasta el Bloque
     * 11</strong>: la tabla referencia {@code negocio} y el directorio todavia no
     * existe. Se construye ahora porque el coste es este metodo y la alternativa
     * era dejar el requisito a medias.</p>
     */
    @Transactional
    public boolean registrarNegocio(UUID negocioId, Interaccion interaccion,
                                    HttpServletRequest peticion) {
        LocalDate hoy = TiempoAyacucho.hoy(clock);
        String huella = huellas.de(peticion);

        String clave = "visita:negocio:" + hoy + ":" + interaccion + ":" + negocioId + ":" + huella;
        try {
            if (!Boolean.TRUE.equals(
                    redis.opsForValue().setIfAbsent(clave, "1", VENTANA_ANTI_RECARGA))) {
                return false;
            }
        } catch (Exception e) {
            log.warn("Sin Redis no se cuentan visitas de negocio: {}", e.getMessage());
            return false;
        }

        // La columna a incrementar depende de la interaccion, y como es un enum
        // propio no hay forma de que llegue texto de fuera a la consulta.
        String columna = switch (interaccion) {
            case VISITA -> "total_visitas";
            case WHATSAPP -> "clics_whatsapp";
            case COMO_LLEGAR -> "clics_como_llegar";
        };

        jdbc.update("""
                INSERT INTO visita_negocio_diario (id, negocio_id, fecha, %s)
                VALUES (?, ?, ?, 1)
                ON CONFLICT (negocio_id, fecha) DO UPDATE
                SET %s = visita_negocio_diario.%s + 1, updated_at = NOW()
                """.formatted(columna, columna, columna), UUID.randomUUID(), negocioId, hoy);

        return true;
    }

    /** Lo que se puede hacer en la ficha de un negocio. */
    public enum Interaccion { VISITA, WHATSAPP, COMO_LLEGAR }
}
