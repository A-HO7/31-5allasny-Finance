package com.team31.financetracker.budget.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Chain of Responsibility (DP-3) — Step 3: extract uid and role claims.
 * No PG user-check is performed in the budget-service; we only validate
 * that the required claims are present.
 */
public class UserLoaderHandler extends AuthHandler {

    private static final Logger log = LoggerFactory.getLogger(UserLoaderHandler.class);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       FilterChain chain) throws IOException, ServletException {

        Claims claims = (Claims) request.getAttribute("jwt.claims");

        if (claims == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Claims not found in request context\"}");
            return;
        }

        // Extract uid — may be stored as "uid", "userId", or "sub"
        Object uidRaw = claims.get("uid");
        if (uidRaw == null) uidRaw = claims.get("userId");
        if (uidRaw == null) uidRaw = claims.getSubject();

        Long uid = null;
        if (uidRaw != null) {
            try {
                uid = Long.parseLong(uidRaw.toString());
            } catch (NumberFormatException e) {
                log.warn("Could not parse uid from JWT: {}", uidRaw);
            }
        }

        // Extract role — may be "role" or "roles"
        String role = null;
        Object roleRaw = claims.get("role");
        if (roleRaw == null) roleRaw = claims.get("roles");
        if (roleRaw != null) {
            role = roleRaw.toString().toUpperCase();
        }

        // Store extracted values as request attributes
        request.setAttribute("jwt.uid", uid);
        request.setAttribute("jwt.role", role);

        if (next != null) {
            next.handle(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }
}
