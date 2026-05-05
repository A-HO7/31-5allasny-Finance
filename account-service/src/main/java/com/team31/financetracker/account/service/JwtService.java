package com.team31.financetracker.account.service;

import com.team31.financetracker.account.config.JwtConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private SecretKey getSigningKey() {
        byte[] keyBytes = JwtConfigurationManager.getInstance().getSecret().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)  // parses a signed JWS and verifies signature
                    .getPayload();
        } catch (JwtException e) {
            // rethrow or wrap to allow upper layers to handle authentication failures
            throw e;
        }
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);

        Object uid = claims.get("uid");
        if (uid == null) uid = claims.get("userId");
        if (uid == null) uid = claims.get("id");

        if (uid == null) return null;
        if (uid instanceof Number n) return n.longValue();
        return Long.parseLong(uid.toString());
    }

    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date exp = claims.getExpiration();
            return exp != null && exp.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // log e if needed
            return false;
        }
    }
}