package com.team31.financetracker.account.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class CacheInvalidator {
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheInvalidator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Section 4.4.6: Wildcard key deletion
    public void invalidateAccountData(Long id) {
        // Delete entity detail
        redisTemplate.delete("account-service::account::" + id);

        // Delete all feature keys for this service
        Set<String> keys = redisTemplate.keys("account-service::S2-F*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}