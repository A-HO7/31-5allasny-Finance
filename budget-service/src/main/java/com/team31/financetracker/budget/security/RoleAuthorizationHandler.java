package com.team31.financetracker.budget.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

/**
 * Chain of Responsibility (DP-3) — Step 4: verify the user has a valid role
 * and set the Spring Security authentication context.
 */
public class RoleAuthorizationHandler extends AuthHandler {

    private static final Logger log = LoggerFactory.getLogger(RoleAuthorizationHandler.class);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       FilterChain chain) throws IOException, ServletException {

        Long uid = (Long) request.getAttribute("jwt.uid");
        String role = (String) request.getAttribute("jwt.role");

        if (uid == null) {
            log.warn("No uid found in JWT claims — rejecting request");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Missing user identifier in token\"}");
            return;
        }

        // Build Spring Security authentication token
        String authority = (role != null) ? "ROLE_" + role : "ROLE_USER";
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        uid.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );
        SecurityContextHolder.getContext().setAuthentication(authToken);

        if (next != null) {
            next.handle(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }
}
