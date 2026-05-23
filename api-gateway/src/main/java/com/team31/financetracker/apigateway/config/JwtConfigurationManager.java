package com.team31.financetracker.apigateway.config;

/**
 * M2 §3.6 — GoF Singleton for shared JWT configuration.
 * <p>
 * This class is NOT a Spring bean (no @Component / @Service / @Configuration).
 * Config is loaded from environment variables with sensible fallback defaults.
 * Thread-safety is guaranteed via double-checked locking.
 */
public class JwtConfigurationManager {

    private static volatile JwtConfigurationManager instance;

    private final String secret;
    private final long expirationMs;

    private JwtConfigurationManager() {
        String envSecret = System.getenv("JWT_SECRET");
        String envExp    = System.getenv("JWT_EXPIRATION_MS");

        this.secret = (envSecret != null && !envSecret.isBlank())
                ? envSecret
                : "StandardSecretKeyForACLProject2026_32Bytes";

        this.expirationMs = (envExp != null && !envExp.isBlank())
                ? Long.parseLong(envExp)
                : 86400000L;
    }

    public static JwtConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (JwtConfigurationManager.class) {
                if (instance == null) {
                    instance = new JwtConfigurationManager();
                }
            }
        }
        return instance;
    }

    public String getSecret() {
        return secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
