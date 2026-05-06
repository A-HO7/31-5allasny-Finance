package com.team31.financetracker.user.config.auth.handlers;

import com.team31.financetracker.user.config.auth.AuthContext;
import com.team31.financetracker.user.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

public class UserLoaderHandler extends AuthHandler {

    private final UserRepository userRepository;

    public UserLoaderHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean process(AuthContext context) throws ServletException, IOException {
        String email = context.getUserEmail();
        
        return userRepository.findByEmail(email).map(user -> {
            // Set up Spring Security context so the rest of the app knows who is logged in
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
            context.setAuthenticatedUser(user);
            return true;
        }).orElseGet(() -> {
            try {
                context.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                context.getResponse().getWriter().write("User not found");
            } catch (IOException e) {
                // Ignore io exception on writing
            }
            return false;
        });
    }
}
