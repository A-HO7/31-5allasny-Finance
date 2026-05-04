package com.team31.financetracker.reporting.config.auth.handlers;

import com.team31.financetracker.reporting.config.JwtService;
import com.team31.financetracker.reporting.config.auth.AuthContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handler 2 of 4 — verifies the JWT signature and expiry using JwtService.
 *
 * On success: populates userEmail from the token subject.
 * On failure: writes HTTP 401 and halts the chain.
 *
 * Uses JwtService (which internally calls JwtConfigurationManager.getInstance()
 * — the DP-5 Singleton — to obtain the signing key).
 */
public class SignatureValidationHandler extends AuthHandler {

    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean process(AuthContext context) throws ServletException, IOException {
        try {
            if (!jwtService.isTokenValid(context.getToken())) {
                context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                context.getResponse().getWriter().write("Invalid or expired JWT token");
                return false;
            }

            Claims claims = jwtService.extractAllClaims(context.getToken());
            context.setUserEmail(claims.getSubject());
            return true;

        } catch (Exception e) {
            context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            context.getResponse().getWriter().write("Invalid or expired JWT token");
            return false;
        }
    }
}
