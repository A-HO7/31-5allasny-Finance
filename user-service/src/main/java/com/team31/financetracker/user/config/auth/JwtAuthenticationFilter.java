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

        String path = request.getRequestURI();
        // SPEC FIX: The spec says /api/auth/register and /api/auth/login are public.
        // Your previous code had /api/users/health (check your controller mapping)
        if (path.startsWith("/api/auth/") || path.contains("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthContext context = new AuthContext(request, response, filterChain);

            AuthHandler chain = new TokenExtractionHandler();
            chain.setNext(new SignatureValidationHandler(jwtService))
                    .setNext(new UserLoaderHandler(userRepository))
                    .setNext(new OwnershipHandler())
                    .setNext(new RoleAuthorizationHandler());

            if (chain.handle(context)) {
                // CRITICAL ADDITION: You must set the authentication in the SecurityContext
                // Use the User loaded by your UserLoaderHandler
                if (context.getAuthenticatedUser() != null) {
                    org.springframework.security.authentication.UsernamePasswordAuthenticationToken authToken =
                            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                    context.getAuthenticatedUser(),
                                    null,
                                    context.getAuthenticatedUser().getAuthorities() // Ensure your User model implements UserDetails correctly
                            );

                    authToken.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authToken);
                }

                filterChain.doFilter(request, response);
            }
        } catch (Exception e) {
            // Log the error to your console so you can see why it's 500ing
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Authentication failed\"}");
        }
    }
}
