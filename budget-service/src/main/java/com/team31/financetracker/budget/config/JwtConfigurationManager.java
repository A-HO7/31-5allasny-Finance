package com.team31.financetracker.budget.config;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Singleton Pattern (DP-5) — NOT a Spring bean.
 * Holds the JWT secret and expiration centrally for the budget-service.
 *
 * <p>Initialised once via {@link #init(String, long)} and thereafter accessible
 * through {@link #getInstance()}.
 */
public final class JwtConfigurationManager {

    private static volatile JwtConfigurationManager INSTANCE;

    private final SecretKey secretKey;
    private final long expirationMs;

    private JwtConfigurationManager(String base64Secret, long expirationMs) {
        SecretKey key;
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
            key = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            // Fallback: generate random key so all external tokens are rejected
            key = io.jsonwebtoken.Jwts.SIG.HS256.key().build();
        }
        this.secretKey = key;
        this.expirationMs = expirationMs;
    }

    /**
     * Initialise the singleton with the given secret and expiration.
     * Thread-safe via double-checked locking.
     */
    public static JwtConfigurationManager init(String base64Secret, long expirationMs) {
        if (INSTANCE == null) {
            synchronized (JwtConfigurationManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new JwtConfigurationManager(base64Secret, expirationMs);
                }
            }
        }
        return INSTANCE;
    }

    public static JwtConfigurationManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("JwtConfigurationManager has not been initialised. Call init() first.");
        }
        return INSTANCE;
    }

    /** Visible-for-testing: allows resetting the singleton in test contexts. */
    public static void reset() {
        synchronized (JwtConfigurationManager.class) {
            INSTANCE = null;
        }
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
