package com.team31.financetracker.reporting.config.auth.handlers;

import com.team31.financetracker.reporting.config.JwtService;
import com.team31.financetracker.reporting.config.auth.AuthContext;
import com.team31.financetracker.reporting.repository.SavedReportRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.Collections;

/**
 * Handler 3 of 4 — loads user identity from JWT claims and sets up the
 * Spring SecurityContext.
 *
 * KEY DIFFERENCE from user-service: The reporting-service has NO local User
 * entity or UserRepository. Instead of hitting a DB, this handler extracts
 * userId and role directly from the already-validated JWT token.
 *
 * On success: sets userId, role, userDetails in AuthContext and populates
 *             the Spring SecurityContextHolder.
 * On failure: writes HTTP 401 and halts the chain.
 */
public class UserLoaderHandler extends AuthHandler {

    private final JwtService jwtService;
    private final SavedReportRepository savedReportRepository;

    public UserLoaderHandler(JwtService jwtService, SavedReportRepository savedReportRepository) {
        this.jwtService = jwtService;
        this.savedReportRepository = savedReportRepository;
    }

    @Override
    protected boolean process(AuthContext context) throws ServletException, IOException {
        try {
            String token = context.getToken();

            Long   userId = jwtService.extractUserId(token);
            String role   = jwtService.extractRole(token);
            String email  = context.getUserEmail(); // already set by SignatureValidationHandler

            if (userId == null || role == null) {
                context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                context.getResponse().getWriter().write("Token is missing required claims");
                return false;
            }

            // Test e) requirement: Verify user actually exists in PG database
            // If the user was deleted after the token was issued, block access.
            if (!savedReportRepository.existsUserById(userId)) {
                context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                context.getResponse().getWriter().write("User associated with token no longer exists");
                return false;
            }

            // Populate context for downstream handlers and controllers
            context.setUserId(userId);
            context.setRole(role);

            // Build a Spring Security UserDetails from JWT claims (no DB needed)
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
            User userDetails = new User(
                    email != null ? email : userId.toString(),
                    "",   // no password needed — token is already validated
                    Collections.singletonList(authority)
            );
            context.setUserDetails(userDetails);

            // Register authentication in Spring SecurityContextHolder
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            SecurityContextHolder.getContext().setAuthentication(authToken);

            return true;

        } catch (Exception e) {
            context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            context.getResponse().getWriter().write("Failed to load user from token");
            return false;
        }
    }
}
