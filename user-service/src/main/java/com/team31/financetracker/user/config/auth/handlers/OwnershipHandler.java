package com.team31.financetracker.user.config.auth.handlers;

import com.team31.financetracker.user.config.auth.AuthContext;
import com.team31.financetracker.user.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OwnershipHandler extends AuthHandler {

    @Override
    protected boolean process(AuthContext context) throws ServletException, IOException {
        String requestURI = context.getRequest().getRequestURI();

        // This regex extracts the {id} from paths like /api/users/123/activity or /api/users/123/profile
        Pattern pattern = Pattern.compile("^/api/users/(\\d+)/(activity|profile|preferences|role)$");
        Matcher matcher = pattern.matcher(requestURI);

        if (matcher.find()) {
            String pathId = matcher.group(1);
            User authenticatedUser = context.getAuthenticatedUser(); // Assuming you added this to AuthContext

            // Rule: Path ID must match User ID, or user must be ADMIN
            boolean isOwner = authenticatedUser.getId().toString().equals(pathId);
            boolean isAdmin = authenticatedUser.getRole().name().equals("ADMIN");

            if (!isOwner && !isAdmin) {
                context.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
                context.getResponse().getWriter().write("Access denied: You do not own this resource");
                return false;
            }
        }
        return true;
    }
}