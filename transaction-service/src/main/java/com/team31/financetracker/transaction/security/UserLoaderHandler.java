package com.team31.financetracker.transaction.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.IOException;

public class UserLoaderHandler extends AuthHandler {

    private final JdbcTemplate jdbcTemplate;

    public UserLoaderHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) throws IOException {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE id = ?",
                    Integer.class, ctx.getUserId());

            if (count == null || count == 0) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"User no longer exists\"}");
                return false;
            }
        } catch (Exception e) {
            // Soft dependency: if DB is unavailable, pass through rather than blocking
            // This prevents cascading 401s during DB startup
        }
        return handleNext(ctx, response);
    }
}