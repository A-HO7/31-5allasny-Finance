package com.team31.financetracker.account.config.auth;

import com.team31.financetracker.account.service.JwtService;
import com.team31.financetracker.account.config.auth.handlers.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Bypass the JWT chain for public endpoints
        if (path.startsWith("/api/accounts/health") || path.equals("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Build and execute the CoR chain
        AuthContext context = new AuthContext(request, response, filterChain);

        AuthHandler chain = new TokenExtractionHandler();
        chain.setNext(new SignatureValidationHandler(jwtService))
                .setNext(new UserLoaderHandler(jwtService))
                .setNext(new RoleAuthorizationHandler());

        boolean success = chain.handle(context);

        // Only continue the Spring Security filter chain if all handlers passed
        //boolean success = chain.handle(context);

        if (success) {
            // 1. Create authorities with the "ROLE_" prefix[cite: 5]
            // Note: ensure context.getRole() matches your AuthContext method name
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + context.getRole()));

            // 2. Create the Authentication token[cite: 5]
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    context.getUserEmail(), // Updated to match your AuthContext
                    null,
                    authorities
            );

            // 3. Attach the numeric userId so your ownership checks can use it[cite: 1, 5]
            authentication.setDetails(context.getUserId());

            // 4. Officially "sign in" the user for this request[cite: 1, 5]
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        }
    }
}
