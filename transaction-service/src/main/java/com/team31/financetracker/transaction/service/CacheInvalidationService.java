package com.team31.financetracker.transaction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Centralised Redis cache invalidation helper for the Transaction Service.
 *
 * Uses the SCAN + DEL (wildcard) strategy described in Section 4.4.6:
 *   - Entity detail:   transaction-service::transaction::{id}
 *   - Feature result:  transaction-service::S3-F{m}::*
 *
 * Called by TransactionService after every write operation.
 *
 * Soft dependency: any Redis failure is caught and logged at WARN level so
 * that a Redis outage never breaks the main write path.
 */
@Service
public class CacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationService.class);
    private static final String PREFIX = "transaction-service::";

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheInvalidationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ── Public invalidation methods ───────────────────────────────────────────

    /**
     * Invalidate the CRUD GET-by-ID cache entry for one transaction.
     * Called by: PUT /api/transactions/{id}, DELETE /api/transactions/{id}
     */
    public void evictTransactionDetail(Long transactionId) {
        deleteKey(PREFIX + "transaction::" + transactionId);
    }

    /**
     * Wildcard-delete all cached results for F1 (search by status/date).
     * Called by: any write that creates/updates/deletes a transaction.
     */
    public void evictF1() {
        deleteByPattern(PREFIX + "S3-F1::*");
    }

    /**
     * Wildcard-delete all cached results for F3 (transfer estimate).
     * Called by: any write that changes account balances.
     */
    public void evictF3() {
        deleteByPattern(PREFIX + "S3-F3::*");
    }

    /**
     * Wildcard-delete all cached results for F5 (metadata search).
     * Called by: any write that creates/updates/deletes a transaction.
     */
    public void evictF5() {
        deleteByPattern(PREFIX + "S3-F5::*");
    }

    /**
     * Wildcard-delete all cached results for F6 (analytics).
     * Called by: any write that creates/updates/deletes a transaction.
     */
    public void evictF6() {
        deleteByPattern(PREFIX + "S3-F6::*");
    }

    /**
     * Wildcard-delete all cached results for F9 (transaction details DTO).
     * Called by: PUT, DELETE on a specific transaction.
     */
    public void evictF9(Long transactionId) {
        deleteByPattern(PREFIX + "S3-F9::" + transactionId + "*");
    }

    /**
     * Wildcard-delete all cached results for S3-F10 (analytics dashboard).
     * Called by: any write per Section 4.4.4 M2-writes rules.
     */
    public void evictF10() {
        deleteByPattern(PREFIX + "S3-F10::*");
    }

    /**
     * Wildcard-delete all cached results for S3-F12 (category recommendations).
     * Called by: S3-F11 record-pattern write per Section 4.4.4 NoSQL-writer rules.
     */
    public void evictF12() {
        deleteByPattern(PREFIX + "S3-F12::*");
    }

    /**
     * Full invalidation triggered by any transaction write (create/update/delete).
     * Covers: detail cache + F1 + F5 + F6 + F9 + F10 (per Section 4.4.4).
     */
    public void evictAllTransactionCaches(Long transactionId) {
        evictTransactionDetail(transactionId);
        evictF1();
        evictF3();
        evictF5();
        evictF6();
        evictF9(transactionId);
        evictF10();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void deleteKey(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Cache evicted: {}", key);
        } catch (Exception ex) {
            log.warn("Cache eviction failed for key [{}]: {}", key, ex.getMessage());
        }
    }

    private void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Cache evicted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception ex) {
            log.warn("Cache wildcard eviction failed for pattern [{}]: {}", pattern, ex.getMessage());
        }
    }
}
