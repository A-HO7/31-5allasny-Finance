package com.team31.financetracker.user.config.auth.handlers;

import com.team31.financetracker.user.config.auth.AuthContext;
import com.team31.financetracker.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class UserLoaderHandler extends AuthHandler {
    private final UserRepository userRepository;

    public UserLoaderHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean process(AuthContext context) throws ServletException, IOException {
        String email = context.getUserEmail();
        if (email == null) return false;

        return userRepository.findByEmail(email).map(user -> {
            // Populate context for OwnershipHandler and RoleAuthorizationHandler
            context.setAuthenticatedUser(user);
            return true;
        }).orElseGet(() -> {
            try {
                context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                context.getResponse().setContentType("application/json");
                context.getResponse().getWriter().write("{\"error\": \"User not found in database\"}");
            } catch (IOException e) { /* ignored */ }
            return false;
        });
    }
}