package com.team31.financetracker.reporting.config.auth.handlers;

import com.team31.financetracker.reporting.config.auth.AuthContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handler 1 of 4 — extracts the Bearer token from the Authorization header.
 *
 * On success: stores the raw token string in the AuthContext.
 * On failure: writes HTTP 401 and halts the chain.
 */
public class TokenExtractionHandler extends AuthHandler {

    @Override
    protected boolean process(AuthContext context) throws ServletException, IOException {
        String authHeader = context.getRequest().getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            context.getResponse().getWriter().write("Missing or invalid Authorization header");
            return false;
        }

        // Strip "Bearer " prefix (7 chars) and pass to next handler
        context.setToken(authHeader.substring(7));
        return true;
    }
}
