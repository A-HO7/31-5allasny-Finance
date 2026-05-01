package com.team31.financetracker.budget.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Chain of Responsibility Pattern (DP-3) — abstract handler.
 * Each concrete handler either processes the request or delegates
 * to the next handler in the chain.
 */
public abstract class AuthHandler {

    protected AuthHandler next;

    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;  // fluent API for chaining
    }

    /**
     * Process the request. If this handler cannot or should not handle it,
     * delegate to {@code next}. If {@code next} is null, call
     * {@code chain.doFilter()} to continue the Spring filter pipeline.
     */
    public abstract void handle(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain chain) throws IOException, ServletException;
}
