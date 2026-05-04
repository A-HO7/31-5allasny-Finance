package com.team31.financetracker.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default configuration (e.g., for M1 Feature DTOs - 10 min)
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Section 8.1: Search results - 5 minutes
        cacheConfigurations.put("account-service::S2-F1", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("account-service::S2-F10", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)));

        // Section 8.1: Entity detail views - 15 minutes
        cacheConfigurations.put("account-service::account", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(15)));

        // Section 8.1: Dashboards/analytics - 10 minutes
        cacheConfigurations.put("account-service::S2-F12", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}