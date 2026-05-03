package com.team31.financetracker.account.config.auth.handlers;

import com.team31.financetracker.account.config.auth.AuthContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Objects;

public class RoleAuthorizationHandler extends AuthHandler {

    @Override
    protected boolean process(AuthContext context) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            context.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
            context.getResponse().getWriter().write("Not authenticated");
            return false;
        }

        String requestURI = context.getRequest().getRequestURI();
        String method     = context.getRequest().getMethod();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        // Replace with endpoints that require ADMIN role in account-service
        boolean isAdminEndpoint =
                requestURI.startsWith("") ||
                requestURI.startsWith("") ||
                requestURI.startsWith("");

        if (isAdminEndpoint && !isAdmin) {
            context.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
            context.getResponse().getWriter().write("Insufficient permissions: ADMIN role required");
            return false;
        }

        return true;
    }
}
