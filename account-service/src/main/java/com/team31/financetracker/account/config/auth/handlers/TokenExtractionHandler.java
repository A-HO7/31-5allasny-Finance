package com.team31.financetracker.account.config.auth.handlers;

import com.team31.financetracker.account.config.auth.AuthContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TokenExtractionHandler extends AuthHandler {

    @Override
    protected boolean process(AuthContext context) throws ServletException, IOException {
        String authHeader = context.getRequest().getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            context.getResponse().getWriter().write("Missing or invalid Authorization header");
            return false;
        }

        context.setToken(authHeader.substring(7));
        return true;
    }
}
