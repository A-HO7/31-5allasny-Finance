package com.team31.financetracker.budget.security;

import com.team31.financetracker.budget.config.JwtConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;
import javax.crypto.SecretKey;

/**
 * Spring-managed utility that delegates to the {@link JwtConfigurationManager}
 * Singleton (DP-5). On construction it initialises the singleton if needed.
 */
@Component
@Lazy(false)
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration:86400000}") long expiration) {
        JwtConfigurationManager mgr = JwtConfigurationManager.init(secret, expiration);
        this.secretKey = mgr.getSecretKey();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse and return the JWT claims. Returns null if the token is invalid.
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
