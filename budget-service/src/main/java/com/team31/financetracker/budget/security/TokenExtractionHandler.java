package com.team31.financetracker.budget.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Chain of Responsibility (DP-3) — Step 1: extract the Bearer token
 * from the Authorization header.
 */
public class TokenExtractionHandler extends AuthHandler {

    private static final Logger log = LoggerFactory.getLogger(TokenExtractionHandler.class);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       FilterChain chain) throws IOException, ServletException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            log.warn("Empty Bearer token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Empty Bearer token\"}");
            return;
        }

        // Store token in request attribute for downstream handlers
        request.setAttribute("jwt.token", token);

        if (next != null) {
            next.handle(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }
}
