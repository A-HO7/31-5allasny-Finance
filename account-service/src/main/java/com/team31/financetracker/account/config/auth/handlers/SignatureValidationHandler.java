package com.team31.financetracker.account.config.auth.handlers;

import com.team31.financetracker.account.config.auth.AuthContext;
import com.team31.financetracker.account.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

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
