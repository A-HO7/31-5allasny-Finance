package com.team31.financetracker.user.config.auth;

import com.team31.financetracker.user.config.auth.handlers.*;
import com.team31.financetracker.user.repository.UserRepository;
import com.team31.financetracker.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Skip filtering for public endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/users/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthContext context = new AuthContext(request, response, filterChain);

            AuthHandler chain = new TokenExtractionHandler();
            chain.setNext(new SignatureValidationHandler(jwtService))
                    .setNext(new UserLoaderHandler(userRepository))
                    .setNext(new OwnershipHandler())       // Added Ownership Check
                    .setNext(new RoleAuthorizationHandler()); // Role check last

            boolean success = chain.handle(context);
            if (success) {
                filterChain.doFilter(request, response);
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Auth error");
        }
    }
}
