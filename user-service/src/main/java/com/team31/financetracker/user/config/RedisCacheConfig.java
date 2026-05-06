package com.team31.financetracker.user.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();  // ← fixed

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableKeyPrefix()
                .entryTtl(Duration.ofMinutes(10));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("userDetailCache", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("s1f1Cache", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("s1f3Cache", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("s1f5Cache", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("s1f6Cache", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("s1f8Cache", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("s1f9Cache", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("s1f12Cache", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}