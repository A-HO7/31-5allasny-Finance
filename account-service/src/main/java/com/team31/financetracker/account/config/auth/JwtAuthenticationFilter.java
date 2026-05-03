package com.team31.financetracker.account.config.auth;

import com.team31.financetracker.account.service.JwtService;
import com.team31.financetracker.account.config.auth.handlers.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JdbcTemplate jdbcTemplate;

    public JwtAuthenticationFilter(JwtService jwtService, JdbcTemplate jdbcTemplate) {
        this.jwtService = jwtService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Bypass the JWT chain for public endpoints
        if (path.startsWith("/api/accounts/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Build and execute the CoR chain
        AuthContext context = new AuthContext(request, response, filterChain);

        AuthHandler chain = new TokenExtractionHandler();
        chain.setNext(new SignatureValidationHandler(jwtService))
                .setNext(new UserLoaderHandler(jwtService, jdbcTemplate))
                .setNext(new RoleAuthorizationHandler());

        boolean success = chain.handle(context);

        // Only continue the Spring Security filter chain if all handlers passed
        if (success) {
            filterChain.doFilter(request, response);
        }
    }
}
