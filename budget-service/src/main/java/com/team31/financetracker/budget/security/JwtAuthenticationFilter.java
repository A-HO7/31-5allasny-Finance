package com.team31.financetracker.budget.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security filter that delegates JWT authentication to the
 * Chain of Responsibility (DP-3): TokenExtraction → SignatureValidation
 * → UserLoader → RoleAuthorization.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthHandler chainHead;

    public JwtAuthenticationFilter() {
        // Build the handler chain
        TokenExtractionHandler extraction = new TokenExtractionHandler();
        SignatureValidationHandler signature = new SignatureValidationHandler();
        UserLoaderHandler loader = new UserLoaderHandler();
        RoleAuthorizationHandler authorization = new RoleAuthorizationHandler();

        extraction.setNext(signature)
                  .setNext(loader)
                  .setNext(authorization);

        this.chainHead = extraction;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        chainHead.handle(request, response, filterChain);
    }
}
