package com.team31.financetracker.transaction.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration for the Transaction Service.
 *
 * TTLs per Section 4.4.1 and Section 8.1:
 *   F1 search results  → 5  min
 *   F3 estimate (POST) → 5  min
 *   F5 metadata search → 5  min
 *   F6 analytics       → 10 min
 *   F9 details DTO     → 10 min
 *   GET-by-ID (CRUD)   → 15 min
 */
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;

@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                // F1 — search (5 min)
                .withCacheConfiguration("transaction-service::S3-F1",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // F3 — transfer estimate (5 min)
                .withCacheConfiguration("transaction-service::S3-F3",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // F5 — metadata search (5 min)
                .withCacheConfiguration("transaction-service::S3-F5",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // F6 — analytics (10 min)
                .withCacheConfiguration("transaction-service::S3-F6",
                        defaultConfig.entryTtl(Duration.ofMinutes(10)))
                // F9 — transaction details DTO (10 min)
                .withCacheConfiguration("transaction-service::S3-F9",
                        defaultConfig.entryTtl(Duration.ofMinutes(10)))
                // CRUD GET-by-ID (15 min)
                .withCacheConfiguration("transaction-service::transaction",
                        defaultConfig.entryTtl(Duration.ofMinutes(15)))
                // M2 features
                .withCacheConfiguration("transaction-service::S3-F10",
                        defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("transaction-service::S3-F12",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache GET error for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache PUT error for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache EVICT error for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Cache CLEAR error: {}", exception.getMessage());
            }
        };
    }
}
