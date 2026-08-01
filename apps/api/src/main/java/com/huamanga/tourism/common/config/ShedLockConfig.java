package com.huamanga.tourism.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Lock distribuido para las tareas programadas.
 *
 * <p>El backend es stateless y puede escalar a N instancias. Sin lock, las N
 * ejecutarian el mismo job a la vez: el refresco de la vista materializada se
 * repetiria y la futura limpieza de tokens competiria consigo misma. ShedLock
 * garantiza que solo una lo ejecute (RNF-39).</p>
 *
 * <p>El lock vive en Redis, el mismo que ya se usa para cache y rate limiting,
 * asi que no hace falta una tabla extra en PostgreSQL.</p>
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory redisConnectionFactory) {
        // El prefijo aisla las claves de este proyecto dentro de Redis.
        return new RedisLockProvider(redisConnectionFactory, "yachay");
    }
}
