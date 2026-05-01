package com.team31.financetracker.budget.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        SecretKey key;
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            key = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            // Unconfigured environment — generate a random key so all tokens are invalid
            key = Jwts.SIG.HS256.key().build();
        }
        this.secretKey = key;
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
}
