package com.team31.financetracker.budget.security;

import com.team31.financetracker.budget.config.JwtConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Chain of Responsibility (DP-3) — Step 2: validate the JWT signature
 * using the shared secret from {@link JwtConfigurationManager}.
 */
public class SignatureValidationHandler extends AuthHandler {

    private static final Logger log = LoggerFactory.getLogger(SignatureValidationHandler.class);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       FilterChain chain) throws IOException, ServletException {

        String token = (String) request.getAttribute("jwt.token");

        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Token not found in request context\"}");
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(JwtConfigurationManager.getInstance().getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Store parsed claims for downstream handlers
            request.setAttribute("jwt.claims", claims);

        } catch (Exception e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid or expired JWT token\"}");
            return;
        }

        if (next != null) {
            next.handle(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }
}
