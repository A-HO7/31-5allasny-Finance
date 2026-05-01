package com.team31.financetracker.reporting.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Spring bean that validates and extracts claims from JWTs issued by user-service.
 *
 * Important: The reporting-service never ISSUES tokens — it only VALIDATES them.
 * All claim extraction methods delegate to extractAllClaims(), which uses the
 * shared secret from JwtConfigurationManager.getInstance() (DP-5 Singleton).
 *
 * Convenience methods required by the CoR handlers (Phase 2 Branch 2):
 *   - extractEmail()   — used by SignatureValidationHandler to identify caller
 *   - extractUserId()  — used by UserLoaderHandler / ownership checks
 *   - extractRole()    — used by RoleAuthorizationHandler
 *   - isTokenValid()   — used by SignatureValidationHandler as a guard
 */
@Service
public class JwtService {

    // ── Signing key ──────────────────────────────────────────────────────────

    private SecretKey getSigningKey() {
        byte[] keyBytes = JwtConfigurationManager.getInstance().getSecret().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Core parser ──────────────────────────────────────────────────────────

    /**
     * Parses the token and returns all claims.
     * Throws io.jsonwebtoken.JwtException on invalid/expired tokens.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Convenience extractors ───────────────────────────────────────────────

    /**
     * Extracts the email (subject) claim.
     * Corresponds to the "sub" field set by user-service on login/register.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the numeric user ID stored in the "uid" claim.
     */
    public Long extractUserId(String token) {
        Object uid = extractAllClaims(token).get("uid");
        if (uid == null) return null;
        if (uid instanceof Number n) return n.longValue();
        return Long.parseLong(uid.toString());
    }

    /**
     * Extracts the role string stored in the "role" claim (e.g. "ADMIN", "USER").
     */
    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    /**
     * Returns true if the token parses successfully and is not expired.
     * Returns false on any JwtException (bad signature, expired, malformed, etc.).
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
