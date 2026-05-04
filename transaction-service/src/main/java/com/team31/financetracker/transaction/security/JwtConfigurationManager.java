package com.team31.financetracker.transaction.security;

/**
 * GoF Singleton — NOT a Spring bean. Holds shared JWT config.
 * Populated at startup via JwtConfigInitializer (singleton-bridge pattern).
 */
public class JwtConfigurationManager {

    private static final JwtConfigurationManager INSTANCE = new JwtConfigurationManager();

    private String secret = "default-dev-secret-key-must-be-32-bytes!";
    private long expirationMs = 86400000L;

    private JwtConfigurationManager() {}

    public static JwtConfigurationManager getInstance() {
        return INSTANCE;
    }

    /** Called once by JwtConfigInitializer @PostConstruct — bridge from Spring config. */
    public static void initConfig(String secret, long expirationMs) {
        INSTANCE.secret = secret;
        INSTANCE.expirationMs = expirationMs;
    }

    public String getSecret()       { return secret; }
    public long   getExpirationMs() { return expirationMs; }
}
