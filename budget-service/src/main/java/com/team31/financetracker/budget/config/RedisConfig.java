package com.team31.financetracker.budget.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
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
 * Redis cache configuration for the Budget Service.
 *
 * <p>Single cache name {@code "budget-service"}; per-feature namespacing is
 * carried in the @Cacheable key (e.g. {@code "budget::" + #id} or
 * {@code "S4-F3::" + ...}). Combined with Spring's default
 * {@code <cacheName>::<key>} Redis-key format, this yields keys like
 * {@code budget-service::budget::32} that match the
 * {@code KEYS 'budget-service::*'} inspection pattern.</p>
 */
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RedisConfig.class);

    private final RedisConnectionFactory factory;

    public RedisConfig(RedisConnectionFactory factory) {
        this.factory = factory;
        if (factory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory lcf) {
            lcf.afterPropertiesSet();
        }
    }

    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @org.springframework.context.annotation.Primary
    @Override
    public CacheManager cacheManager() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer()))
                .disableCachingNullValues();

        RedisCacheManager mgr = RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("budget-service",
                        defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .build();

        log.info("Built RedisCacheManager: {}", mgr.getClass().getName());
        return mgr;
    }

    @Bean
    public org.springframework.boot.CommandLineRunner cacheManagerDiagnostics(
            org.springframework.context.ApplicationContext ctx) {
        return args -> {
            java.util.Map<String, CacheManager> beans = ctx.getBeansOfType(CacheManager.class);
            log.info("All CacheManager beans:");
            beans.forEach((name, bean) ->
                    log.info("  [{}] -> {}", name, bean.getClass().getName()));

            try {
                CacheManager primary = ctx.getBean(CacheManager.class);
                log.info("Active primary CacheManager: {}", primary.getClass().getName());
            } catch (Exception e) {
                log.info("No active primary CacheManager or multiple found: {}", e.getMessage());
            }
        };
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception,
                                            org.springframework.cache.Cache cache,
                                            Object key) {
                log.warn("Cache GET error for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception,
                                            org.springframework.cache.Cache cache,
                                            Object key,
                                            Object value) {
                log.warn("Cache PUT error for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception,
                                              org.springframework.cache.Cache cache,
                                              Object key) {
                log.warn("Cache EVICT error for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception,
                                              org.springframework.cache.Cache cache) {
                log.warn("Cache CLEAR error: {}", exception.getMessage());
            }
        };
    }
}