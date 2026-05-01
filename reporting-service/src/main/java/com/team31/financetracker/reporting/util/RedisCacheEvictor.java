package com.team31.financetracker.reporting.util;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RedisCacheEvictor {

    private final StringRedisTemplate redisTemplate;

    public RedisCacheEvictor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Deletes all keys matching the given patterns using the SCAN command.
     * SCAN is preferred over KEYS for production performance.
     */
    public void evictByPatterns(String... patterns) {
        for (String pattern : patterns) {
            redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
                try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
                    List<byte[]> keysToDelete = new ArrayList<>();
                    while (cursor.hasNext()) {
                        keysToDelete.add(cursor.next());
                    }
                    if (!keysToDelete.isEmpty()) {
                        connection.del(keysToDelete.toArray(new byte[0][]));
                    }
                } catch (Exception e) {
                    // Fail gracefully so cache issues don't break transactions
                }
                return null;
            });
        }
    }
}
