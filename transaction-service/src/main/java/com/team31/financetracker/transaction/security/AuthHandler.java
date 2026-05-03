package com.team31.financetracker.transaction.security;


import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class AuthHandler {

    protected AuthHandler next;

    /** Returns the argument so callers can chain: a.setNext(b).setNext(c) */
    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;
    }

    /** Returns true if this step passed; false if it wrote an error response. */
    public abstract boolean handle(AuthContext ctx, HttpServletResponse response)
            throws IOException;

    protected boolean handleNext(AuthContext ctx, HttpServletResponse response)
            throws IOException {
        if (next != null) return next.handle(ctx, response);
        return true; // end of chain — all checks passed
    }
}