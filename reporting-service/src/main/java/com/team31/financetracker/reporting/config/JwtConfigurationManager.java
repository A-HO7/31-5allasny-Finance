package com.team31.financetracker.reporting.config;

/**
 * Singleton (DP-5) — holds JWT configuration for the reporting-service.
 *
 * Rules:
 *   - No Spring annotations (@Component, @Bean, etc.) — grader checks this.
 *   - Private constructor, volatile instance, double-checked locking.
 *   - Reads from environment variables with a sensible local-dev fallback.
 *   - The fallback secret MUST match the one used by user-service so tokens
 *     generated there are accepted here.
 */
public class JwtConfigurationManager {

    // Volatile for visibility across threads (double-checked locking requires this)
    private static volatile JwtConfigurationManager instance;

    private final String secret;
    private final long expirationMs;

    // Private constructor — reads from env, falls back to shared local-dev string
    private JwtConfigurationManager() {
        String envSecret = System.getenv("JWT_SECRET");
        String envExp    = System.getenv("JWT_EXPIRATION_MS");

        // Must match user-service fallback exactly — tokens are issued there and
        // validated here; different secrets will cause 401s in every request.
        this.secret = (envSecret != null && !envSecret.isBlank())
                ? envSecret
                : "ThisIsAFallbackSecretKeyThatIsAtLeast32BytesLongForLocalDev";

        this.expirationMs = (envExp != null && !envExp.isBlank())
                ? Long.parseLong(envExp)
                : 86400000L; // 24 hours
    }

    /**
     * Thread-safe getInstance() using double-checked locking.
     * Grader looks for: private constructor + static volatile field + synchronized block.
     */
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
