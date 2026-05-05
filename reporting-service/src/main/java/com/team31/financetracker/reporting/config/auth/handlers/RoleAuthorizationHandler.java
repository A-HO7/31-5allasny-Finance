package com.team31.financetracker.reporting.config.auth.handlers;

import com.team31.financetracker.reporting.config.auth.AuthContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

/**
 * Handler 4 of 4 — enforces role-based access control for reporting endpoints.
 *
 * Rules (reporting-service specific):
 *   - All authenticated users can access their own reports (user-scoped endpoints).
 *   - Analytics endpoints (S5-F10, S5-F11) require ADMIN role.
 *     → GET /api/reports/analytics       — legacy M1 analytics (ADMIN only)
 *     → GET /api/reports/audit            — S5-F11 report generation audit (ADMIN only)
 *     → GET /api/reports/health-score     — S5-F10 financial health score (ADMIN only)
 *   - Any authenticated user can access other GET /api/reports/** endpoints.
 *
 * On failure (wrong role): writes HTTP 403 and halts the chain.
 */
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
        String method     = context.getRequest().getMethod();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Analytics endpoints require ADMIN role, EXCEPT for the Financial Health Score (S5-F10)
        // which allows ownership-based access (validated in the controller).
        boolean isAnalyticsEndpoint =
                (requestURI.startsWith("/api/reports/analytics") && !requestURI.equals("/api/reports/analytics/health")) ||
                requestURI.startsWith("/api/reports/audit")     ||
                requestURI.startsWith("/api/reports/health-score");

        if (isAnalyticsEndpoint && !isAdmin) {
            context.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
            context.getResponse().getWriter().write("Insufficient permissions: ADMIN role required");
            return false;
        }

        return true;
    }
}
