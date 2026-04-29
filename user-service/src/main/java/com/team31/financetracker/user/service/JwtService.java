package com.team31.financetracker.user.service;

import com.team31.financetracker.user.config.JwtConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // Helper to get the secret key in the format JJWT expects
    private SecretKey getSigningKey() {
        byte[] keyBytes = JwtConfigurationManager.getInstance().getSecret().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims) // Updated from setClaims
                .subject(email) // Updated from setSubject
                .issuedAt(new Date(System.currentTimeMillis())) // Updated from setIssuedAt
                .expiration(new Date(System.currentTimeMillis() + JwtConfigurationManager.getInstance().getExpirationMs())) // Updated from setExpiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser() // Changed from parserBuilder()
                .verifyWith(getSigningKey()) // Updated to verifyWith
                .build()
                .parseSignedClaims(token) // Updated from parseClaimsJws
                .getPayload(); // Updated from getBody
    }
}