package com.team31.financetracker.transaction.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SignatureValidationHandler extends AuthHandler {

    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) throws IOException {
        if (!jwtService.isTokenValid(ctx.getToken())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or expired JWT token\"}");
            return false;
        }
        ctx.setUserId(jwtService.extractUserId(ctx.getToken()));
        ctx.setRole(jwtService.extractRole(ctx.getToken()));
        ctx.setEmail(jwtService.extractEmail(ctx.getToken()));
        return handleNext(ctx, response);
    }
}
