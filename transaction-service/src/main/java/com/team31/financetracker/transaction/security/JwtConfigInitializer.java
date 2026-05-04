package com.team31.financetracker.transaction.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfigInitializer {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    @PostConstruct
    public void init() {
        JwtConfigurationManager.initConfig(secret, expirationMs);
    }
}