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

        // 2. Se pregunta al proveedor, protegido por el cortacircuitos.
        Optional<T> nuevo = proveedor.get();
        if (nuevo.isPresent()) {
            guardar(nombre + ":fresco", nuevo.get(), propiedades.ttlFresco());
            guardar(nombre + ":ultimo-bueno", nuevo.get(), propiedades.ttlUltimoBueno());
            return nuevo.get();
        }

        // 3. No respondio: se sirve lo ultimo que se supo, marcado como tal.
        Optional<T> ultimo = leer(nombre + ":ultimo-bueno", tipo);
        if (ultimo.isPresent()) {
            log.info("Sirviendo {} obsoleto: el proveedor no respondio", nombre);
            return obsoleto.apply(ultimo.get());
        }

        // 4. Ni cache ni proveedor. Se dice claramente, sin inventar datos.
        return vacio.get();
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
