package com.team31.financetracker.account.config.auth.handlers;

import com.team31.financetracker.account.config.auth.AuthContext;
import com.team31.financetracker.account.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import java.io.IOException;
import java.util.Collections;

public class UserLoaderHandler extends AuthHandler {

    private final JwtService jwtService;

    public UserLoaderHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean process(AuthContext context) throws IOException {
        try {
            String token = context.getToken();

            Long userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            String email = context.getUserEmail();

            if (userId == null || role == null) {
                context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                context.getResponse().getWriter().write("Token is missing required claims");
                return false;
            }

            context.setUserId(userId);
            context.setRole(role);

            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
            User userDetails = new User(
                    email != null ? email : userId.toString(),
                    "",   // no password needed — token is already validated
                    Collections.singletonList(authority)
            );
            context.setUserDetails(userDetails);

            // Populate Spring Security context
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authentication.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            return true;

        } catch (Exception e) {
            context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            context.getResponse().getWriter().write("Failed to load user from token");
            return false;
        }
    }
}
