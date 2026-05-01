package com.team31.financetracker.reporting.config.auth;

import com.team31.financetracker.reporting.config.JwtService;
import com.team31.financetracker.reporting.config.auth.handlers.*;
import com.team31.financetracker.reporting.repository.SavedReportRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that runs the Chain of Responsibility (DP-3) on every request.
 *
 * Chain order:
 *   TokenExtractionHandler
 *     → SignatureValidationHandler
 *       → UserLoaderHandler
 *         → RoleAuthorizationHandler
 *
 * Public endpoints bypass the chain entirely:
 *   - GET /api/reports/health  (liveness probe)
 *
 * All other requests require a valid Bearer JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SavedReportRepository savedReportRepository;

    public JwtAuthenticationFilter(JwtService jwtService, SavedReportRepository savedReportRepository) {
        this.jwtService = jwtService;
        this.savedReportRepository = savedReportRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Bypass the JWT chain for public endpoints
        if (path.startsWith("/api/reports/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Build and execute the CoR chain
        AuthContext context = new AuthContext(request, response, filterChain);

        AuthHandler chain = new TokenExtractionHandler();
        chain.setNext(new SignatureValidationHandler(jwtService))
             .setNext(new UserLoaderHandler(jwtService, savedReportRepository))
             .setNext(new RoleAuthorizationHandler());

        boolean success = chain.handle(context);

        // Only continue the Spring Security filter chain if all handlers passed
        if (success) {
            filterChain.doFilter(request, response);
        }
    }
}
