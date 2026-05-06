package com.team31.financetracker.user.config.auth.handlers;

import com.team31.financetracker.user.config.auth.AuthContext;
import com.team31.financetracker.user.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OwnershipHandler extends AuthHandler {

    // Locate the process method in OwnershipHandler.java

    @Override
    protected boolean process(AuthContext context) throws ServletException, IOException {
        String requestURI = context.getRequest().getRequestURI();

        // FIXED REGEX: This now catches /api/users/123 AND /api/users/123/activity
        // The previous regex was missing the base update/delete path
        Pattern pattern = Pattern.compile("^/api/users/(\\d+)(/.*)?$");
        Matcher matcher = pattern.matcher(requestURI);

        if (matcher.find()) {
            String pathId = matcher.group(1);
            User authenticatedUser = context.getAuthenticatedUser();

            if (authenticatedUser == null) {
                return true; // Let UserLoaderHandler handle the 401 if user is missing
            }

            boolean isOwner = authenticatedUser.getId().toString().equals(pathId);
            boolean isAdmin = authenticatedUser.getRole().name().equals("ADMIN");

            if (!isOwner && !isAdmin) {
                context.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
                context.getResponse().setContentType("application/json");
                context.getResponse().getWriter().write("{\"error\": \"Access denied: You do not own this resource\"}");
                return false;
            }
        }
        return true;
    }
}