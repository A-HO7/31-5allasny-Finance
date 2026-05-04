package com.team31.financetracker.transaction.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService    jwtService;
    private final JdbcTemplate  jdbcTemplate;
    private final AuthContext   authContext;

    public JwtAuthenticationFilter(JwtService jwtService, JdbcTemplate jdbcTemplate, AuthContext authContext) {
        this.jwtService   = jwtService;
        this.jdbcTemplate = jdbcTemplate;
        this.authContext  = authContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        // ── Build the Chain of Responsibility ──────────────────────────────
        TokenExtractionHandler    tokenExtractor     = new TokenExtractionHandler();
        SignatureValidationHandler signatureValidator = new SignatureValidationHandler(jwtService);
        UserLoaderHandler          userLoader         = new UserLoaderHandler(jdbcTemplate);
        RoleAuthorizationHandler   roleAuthorizer     = new RoleAuthorizationHandler();

        // Fluent chain: each setNext() returns the argument
        tokenExtractor
                .setNext(signatureValidator)
                .setNext(userLoader)
                .setNext(roleAuthorizer);
        // ───────────────────────────────────────────────────────────────────

        boolean passed = tokenExtractor.handle(authContext, response);

        if (passed) {
            // Populate Spring Security context so @PreAuthorize / hasRole work too
            var authToken = new UsernamePasswordAuthenticationToken(
                    authContext.getEmail(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + authContext.getRole()))
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            filterChain.doFilter(request, response);
        }
        // If !passed, a handler already wrote the 401/403 — do NOT call filterChain
    }
}
