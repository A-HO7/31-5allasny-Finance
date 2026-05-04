package com.team31.financetracker.transaction.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TokenExtractionHandler extends AuthHandler {

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) throws IOException {
        String header = ctx.getRequest().getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return false;
        }
        ctx.setToken(header.substring(7));
        return handleNext(ctx, response);
    }
}
