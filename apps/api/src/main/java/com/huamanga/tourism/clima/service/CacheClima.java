package com.huamanga.tourism.clima.service;

import com.huamanga.tourism.clima.config.PropiedadesClima;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cache de dos niveles para el clima (RF-25).
 *
 * <p>Aqui esta la idea central del modulo. Una cache con expiracion protege de
 * llamar de mas al proveedor, pero <strong>no protege de que el proveedor se
 * caiga</strong>: pasados los 30 minutos la entrada desaparece, y si en ese
 * momento OpenWeatherMap no responde no queda nada que servir. Por eso el mismo
 * dato se guarda dos veces:</p>
 *
 * <table>
 *   <tr><td>{@code :fresco}</td><td>30 min</td><td>evita llamadas repetidas</td></tr>
 *   <tr><td>{@code :ultimo-bueno}</td><td>24 h</td><td>sobrevive a la caida</td></tr>
 * </table>
 *
 * <p>Es tambien la razon de no usar {@code @Cacheable}: la anotacion sabe
 * guardar y expirar, pero no sabe «si falla, dame lo ultimo que tuvieras».</p>
 *
 * <p>Un fallo del propio Redis tampoco rompe nada: se registra y se sigue
 * adelante llamando al proveedor. La cache es una optimizacion, nunca un punto
 * unico de fallo.</p>
 */
@Component
public class CacheClima {

    private static final Logger log = LoggerFactory.getLogger(CacheClima.class);

    /**
     * Cuanto vale el permiso para refrescar. Debe superar el tiempo de una
     * llamada al proveedor (unos 2 s con el cortacircuitos) sin llegar a
     * congelar el dato si el proceso que lo tomo desaparece.
     */
    private static final java.time.Duration ESPERA_DEL_TESTIGO = java.time.Duration.ofSeconds(10);

    private final RedisTemplate<String, Object> redis;
    private final PropiedadesClima propiedades;

    public CacheClima(RedisTemplate<String, Object> redisTemplateJson, PropiedadesClima propiedades) {
        this.redis = redisTemplateJson;
        this.propiedades = propiedades;
    }

    /**
     * Resuelve un dato de clima con degradacion elegante.
     *
     * @param nombre    prefijo de la clave (por ejemplo {@code clima:actual})
     * @param tipo      clase esperada, para comprobar lo que vuelve de Redis
     * @param proveedor llamada real al servicio externo
     * @param obsoleto  como marcar el dato cuando se sirve el ultimo conocido
     * @param vacio     que devolver cuando no hay absolutamente nada
     */
    public <T> T resolver(String nombre,
                          Class<T> tipo,
                          Supplier<Optional<T>> proveedor,
                          java.util.function.UnaryOperator<T> obsoleto,
                          Supplier<T> vacio) {

        // 1. ¿Sigue fresco? Entonces no se molesta a nadie.
        Optional<T> fresco = leer(nombre + ":fresco", tipo);
        if (fresco.isPresent()) {
            return fresco.get();
        }

        // 2. Se pregunta al proveedor, pero SOLO UNA PETICION A LA VEZ.
        //
        //    Sin este cerrojo hay estampida de cache: cuando la clave fresca
        //    caduca, todas las peticiones que llegan a la vez fallan la lectura
        //    y todas llaman al proveedor. Con 50 usuarios concurrentes eso son
        //    50 llamadas para traer el mismo dato.
        //
        //    No es teorico: se detecto en las pruebas de carga del Bloque 13.
        //    Al vaciar Redis y lanzar el escenario de cache fria, la rafaga de
        //    llamadas simultaneas agoto la cuota gratuita de OpenWeatherMap y
        //    la cuenta quedo bloqueada temporalmente. En produccion, un pico de
        //    Semana Santa justo al caducar la clave habria hecho lo mismo.
        //
        //    El cerrojo vive en Redis y no en memoria a proposito: con varias
        //    instancias del backend, un lock local dejaria pasar una llamada
        //    por instancia y el problema volveria multiplicado por N.
        if (!tomarElTestigo(nombre)) {
            // Otro hilo ya esta refrescando. En vez de esperar —lo que
            // encolaria peticiones y empeoraria la latencia bajo carga— se
            // sirve el ultimo dato bueno, que es la respuesta correcta y
            // ademas instantanea.
            Optional<T> mientrasTanto = leer(nombre + ":ultimo-bueno", tipo);
            if (mientrasTanto.isPresent()) {
                return obsoleto.apply(mientrasTanto.get());
            }
            return vacio.get();
        }

        Optional<T> nuevo = proveedor.get();
        if (nuevo.isPresent()) {
            guardar(nombre + ":fresco", nuevo.get(), propiedades.ttlFresco());
            guardar(nombre + ":ultimo-bueno", nuevo.get(), propiedades.ttlUltimoBueno());
            soltarElTestigo(nombre);
            return nuevo.get();
        }

        // Si el proveedor fallo NO se suelta el testigo: que caduque solo. Asi
        // no se le lanzan mil reintentos a un servicio que acaba de fallar, que
        // es justo lo que convierte una caida ajena en una tormenta propia.

        // 3. No respondio: se sirve lo ultimo que se supo, marcado como tal.
        Optional<T> ultimo = leer(nombre + ":ultimo-bueno", tipo);
        if (ultimo.isPresent()) {
            log.info("Sirviendo {} obsoleto: el proveedor no respondio", nombre);
            return obsoleto.apply(ultimo.get());
        }

        // 4. Ni cache ni proveedor. Se dice claramente, sin inventar datos.
        return vacio.get();
    }

    /**
     * Intenta quedarse con el permiso para refrescar este dato.
     *
     * <p>{@code SET NX EX} es atomico: de todas las peticiones que lleguen a la
     * vez, exactamente una recibe {@code true}. El testigo caduca solo, de modo
     * que si el proceso que lo tomo muere a mitad, el siguiente lo recoge en
     * unos segundos en vez de dejar el dato congelado para siempre.</p>
     *
     * <p>Si Redis no responde se devuelve {@code true} y se llama al proveedor:
     * es preferible una llamada de mas que quedarse sin clima.</p>
     */
    private boolean tomarElTestigo(String nombre) {
        try {
            Boolean ganado = redis.opsForValue()
                    .setIfAbsent(nombre + ":refrescando", "1", ESPERA_DEL_TESTIGO);
            return Boolean.TRUE.equals(ganado);
        } catch (Exception e) {
            log.warn("No se pudo tomar el testigo de {}: {}", nombre, e.getMessage());
            return true;
        }
    }

    private void soltarElTestigo(String nombre) {
        try {
            redis.delete(nombre + ":refrescando");
        } catch (Exception e) {
            log.warn("No se pudo soltar el testigo de {}: {}", nombre, e.getMessage());
        }
    }

    private <T> Optional<T> leer(String clave, Class<T> tipo) {
        try {
            Object valor = redis.opsForValue().get(clave);
            return tipo.isInstance(valor) ? Optional.of(tipo.cast(valor)) : Optional.empty();
        } catch (Exception e) {
            // Redis caido no puede impedir que se muestre el clima.
            log.warn("No se pudo leer {} de Redis: {}", clave, e.getMessage());
            return Optional.empty();
        }
    }

    private void guardar(String clave, Object valor, java.time.Duration ttl) {
        try {
            redis.opsForValue().set(clave, valor, ttl);
        } catch (Exception e) {
            log.warn("No se pudo guardar {} en Redis: {}", clave, e.getMessage());
        }
    }

    /** Solo para tests y para el panel de administracion. */
    public void invalidar(String nombre) {
        try {
            redis.delete(java.util.List.of(nombre + ":fresco", nombre + ":ultimo-bueno"));
        } catch (Exception e) {
            log.warn("No se pudo invalidar {}: {}", nombre, e.getMessage());
        }
    }
}
