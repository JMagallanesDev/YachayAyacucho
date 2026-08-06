package com.huamanga.tourism.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Plantilla de Redis con serializacion JSON.
 *
 * <p>El serializador por defecto de Spring Data Redis es el de Java, y es
 * inaceptable por dos motivos: escribe binario ilegible —imposible de
 * inspeccionar con {@code redis-cli} cuando algo va mal— y deserializa
 * cualquier clase que llegue, que es la puerta de entrada clasica a la
 * ejecucion remota de codigo si alguien logra escribir en la cache.</p>
 *
 * <p><strong>Nota de version.</strong> El CLAUDE.md pide
 * {@code GenericJackson2JsonRedisSerializer}, que es el de Jackson 2. Spring
 * Data Redis 4.1 incluye ambos, pero este proyecto va en <em>Jackson 3</em>
 * (paquete {@code tools.jackson}), cuyo serializador es
 * {@code GenericJacksonJsonRedisSerializer} —sin el «2»—. Usar el antiguo
 * arrastraria Jackson 2 solo para la cache. Queda anotado como correccion al
 * CLAUDE.md.</p>
 */
@Configuration
public class RedisConfig {

    /**
     * @param <T> tipo del valor cacheado; cada servicio declara el suyo
     */
    @Bean
    public <T> RedisTemplate<String, T> redisTemplateJson(RedisConnectionFactory conexion) {
        RedisTemplate<String, T> plantilla = new RedisTemplate<>();
        plantilla.setConnectionFactory(conexion);

        // Claves en texto plano: es lo que permite listarlas y depurarlas.
        plantilla.setKeySerializer(new StringRedisSerializer());
        plantilla.setHashKeySerializer(new StringRedisSerializer());

        // El builder configura por dentro el JsonMapper de Jackson 3 y anade
        // la informacion de tipo que permite recuperar el objeto en su clase
        // original. Sin ella, todo volveria de Redis como un LinkedHashMap.
        GenericJacksonJsonRedisSerializer json = GenericJacksonJsonRedisSerializer.builder().build();
        plantilla.setValueSerializer(json);
        plantilla.setHashValueSerializer(json);

        plantilla.afterPropertiesSet();
        return plantilla;
    }
}
