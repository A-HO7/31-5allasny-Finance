package com.team31.financetracker.reporting.config.auth.handlers;

import com.team31.financetracker.reporting.config.auth.AuthContext;
import jakarta.servlet.ServletException;
import java.io.IOException;

/**
 * Abstract base for the Chain of Responsibility (DP-3).
 *
 * Each concrete handler implements process() and either:
 *   - returns true  → passes control to the next handler in the chain
 *   - returns false → halts the chain (has already written error response)
 *
 * setNext() returns the next handler so chains can be wired fluently:
 *   handler1.setNext(handler2).setNext(handler3);
 */
public abstract class AuthHandler {

    private AuthHandler next;

    /**
     * Wires the next handler and returns it for fluent chaining.
     */
    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;
    }

    /**
     * Calls process() and, if successful, forwards to the next handler.
     */
    public boolean handle(AuthContext context) throws ServletException, IOException {
        if (process(context)) {
            if (next != null) {
                return next.handle(context);
            }
            return true; // end of chain — all handlers passed
        }
        return false; // this handler failed — halt
    }

    /**
     * Concrete handler logic.
     * Must return true to continue the chain, false to halt it.
     * When returning false the handler is responsible for writing the HTTP error response.
     */
    protected abstract boolean process(AuthContext context) throws ServletException, IOException;
}
