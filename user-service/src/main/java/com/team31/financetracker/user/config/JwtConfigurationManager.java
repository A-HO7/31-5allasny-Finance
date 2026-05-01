package com.team31.financetracker.user.config;

public class JwtConfigurationManager {

    // 1. Single private static field holds the instance (volatile for thread-safety)
    private static volatile JwtConfigurationManager instance;

    // JWT configuration properties
    private final String secret;
    private final long expirationMs;

    // 2. Private constructor
    private JwtConfigurationManager() {
        /*
         * The PDF recommends reading System.getenv() inside the constructor
         * and providing sensible fallbacks for local dev.
         */
        String envSecret = System.getenv("JWT_SECRET");
        String envExp = System.getenv("JWT_EXPIRATION_MS");

        // Secret MUST be at least 32 bytes (256 bits) for HMAC-SHA256 per the PDF!
        this.secret = (envSecret != null && !envSecret.isBlank())
                ? envSecret
                : "ThisIsAFallbackSecretKeyThatIsAtLeast32BytesLongForLocalDev";

        // Default to 24 hours (86400000 ms)
        this.expirationMs = (envExp != null && !envExp.isBlank())
                ? Long.parseLong(envExp)
                : 86400000L;
    }

    // 3. Static getInstance() method with double-checked locking for thread safety
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

    // Getters for the config values
    public String getSecret() {
        return secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}