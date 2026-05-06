package com.team31.financetracker.user.config;

public class JwtConfigurationManager {
    private static volatile JwtConfigurationManager instance;
    private final String secret;
    private final long expirationMs;

    private JwtConfigurationManager() {
        String envSecret = System.getenv("JWT_SECRET");
        String envExp = System.getenv("JWT_EXPIRATION_MS");

        // FIX: Ensure fallback is exactly 32 characters (256 bits) to avoid WeakKeyException
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

    public String getSecret() { return secret; }
    public long getExpirationMs() { return expirationMs; }
}