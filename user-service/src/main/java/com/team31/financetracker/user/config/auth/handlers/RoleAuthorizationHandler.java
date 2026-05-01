package com.team31.financetracker.user.config.auth.handlers;

import com.team31.financetracker.user.config.auth.AuthContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RoleAuthorizationHandler extends AuthHandler {

    @Override
    protected boolean process(AuthContext context) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            context.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
            context.getResponse().getWriter().write("Not authenticated");
            return false;
        }

        String requestURI = context.getRequest().getRequestURI();
        String method = context.getRequest().getMethod();

        boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Role Authorization: PUT /api/users/{id}/role requires ADMIN
        if (requestURI.matches("^/api/users/\\d+/role$") && method.equals("PUT")) {
            if (!hasAdminRole) {
                context.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
                context.getResponse().getWriter().write("Insufficient permissions");
                return false;
            }
        }

        return true;
    }
}
