package com.demo.employee.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

/**
 * Tunes the Redis-backed caches used by {@code EmployeeService}.
 *
 * <p>Entities are cached using JDK serialization (the domain types implement
 * {@link java.io.Serializable}); here we simply attach a time-to-live so cached
 * reads cannot go stale indefinitely.</p>
 */
@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheTtlCustomizer() {
        RedisCacheConfiguration defaults =
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5));
        return builder -> builder.cacheDefaults(defaults);
    }
}
