package com.team31.financetracker.user.service;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CacheInvalidationService {

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheInvalidationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void evictUserDetail(Long userId) {
        safeDelete("user-service::user::" + userId);
    }

    public void evictUserFeatureCaches() {
        safeDeleteByPattern("user-service::S1-F1::*");
        safeDeleteByPattern("user-service::S1-F3::*");
        safeDeleteByPattern("user-service::S1-F5::*");
        safeDeleteByPattern("user-service::S1-F6::*");
        safeDeleteByPattern("user-service::S1-F8::*");
        safeDeleteByPattern("user-service::S1-F9::*");
        safeDeleteByPattern("user-service::S1-F12::*");
    }

    public void evictRoleChangeCaches(Long userId) {
        evictUserDetail(userId);
        safeDeleteByPattern("user-service::S1-F12::" + userId + ":*");
    }

    private void safeDelete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
            // Redis is a soft dependency.
        }
    }

    private void safeDeleteByPattern(String pattern) {
        try {
            redisTemplate.execute((RedisConnection connection) -> {
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
                List<byte[]> keysToDelete = new ArrayList<>();
                try (var cursor = connection.keyCommands().scan(options)) {
                    while (cursor.hasNext()) {
                        keysToDelete.add(cursor.next());
                    }
                }
                if (!keysToDelete.isEmpty()) {
                    connection.keyCommands().del(keysToDelete.toArray(new byte[0][]));
                }
                return null;
            });
        } catch (Exception ignored) {
            // Redis is a soft dependency.
        }
    }
}
