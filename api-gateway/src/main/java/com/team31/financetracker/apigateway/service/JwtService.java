package com.team31.financetracker.apigateway.service;

import com.team31.financetracker.apigateway.config.JwtConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Spring-managed JWT service for the API Gateway.
 * <p>
 * Reads JWT configuration from {@link JwtConfigurationManager#getInstance()} (GoF Singleton)
 * rather than via @Autowired, as required by M2 §3.6.
 * <p>
 * This bean is injected into {@link com.team31.financetracker.apigateway.filter.JwtGatewayFilter}
 * and called inside a {@code Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())}
 * to avoid pinning the Reactor event-loop thread.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService() {
        // Obtain config from the GoF singleton — NOT from Spring @Value
        String secret = JwtConfigurationManager.getInstance().getSecret();
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validate the token signature and expiry, then return all claims.
     * Throws a JwtException (unchecked) on any validation failure —
     * the caller must catch this on a boundedElastic scheduler thread.
     *
     * @param token raw JWT (without "Bearer " prefix)
     * @return parsed {@link Claims}
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
