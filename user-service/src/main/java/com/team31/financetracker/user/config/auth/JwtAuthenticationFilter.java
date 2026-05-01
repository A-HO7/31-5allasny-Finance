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

        // 2. Initialize Context for the Chain
        AuthContext context = new AuthContext(request, response, filterChain);

        // 3. Build the Chain of Responsibility
        AuthHandler chain = new TokenExtractionHandler();
        chain.setNext(new SignatureValidationHandler(jwtService))
                .setNext(new UserLoaderHandler(userRepository))
                .setNext(new RoleAuthorizationHandler());

        // 4. Execute the Chain
        boolean success = chain.handle(context);

        // 5. If all handlers succeeded, continue the Spring Security filter chain
        if (success) {
            filterChain.doFilter(request, response);
        }
    }
}
