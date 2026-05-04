package com.team31.financetracker.transaction.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RoleAuthorizationHandler extends AuthHandler {

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) throws IOException {
        String required = ctx.getRequiredRole();
        // Only enforce if a specific role is required (e.g., ADMIN-only endpoints)
        if ("ADMIN".equals(required) && !"ADMIN".equals(ctx.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Insufficient role — ADMIN required\"}");
            return false;
        }
        return handleNext(ctx, response);
    }
}