package com.team31.financetracker.reporting.config.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Mutable context object passed through the Chain of Responsibility (DP-3).
 *
 * Each handler reads from and writes to this object.
 * Unlike the user-service's AuthContext, this version carries userId and role
 * directly from the JWT claims — there is no local User entity in the
 * reporting-service to load from a database.
 */
public class AuthContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final FilterChain filterChain;

    // Set by TokenExtractionHandler
    private String token;

    // Set by SignatureValidationHandler (from JWT subject)
    private String userEmail;

    // Set by UserLoaderHandler (from JWT "uid" claim)
    private Long userId;

    // Set by UserLoaderHandler (from JWT "role" claim)
    private String role;

    // Set by UserLoaderHandler for SecurityContextHolder
    private UserDetails userDetails;

    public AuthContext(HttpServletRequest request,
                       HttpServletResponse response,
                       FilterChain filterChain) {
        this.request     = request;
        this.response    = response;
        this.filterChain = filterChain;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public HttpServletRequest  getRequest()    { return request; }
    public HttpServletResponse getResponse()   { return response; }
    public FilterChain         getFilterChain(){ return filterChain; }

    public String      getToken()     { return token; }
    public void        setToken(String token) { this.token = token; }

    public String      getUserEmail() { return userEmail; }
    public void        setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Long        getUserId()    { return userId; }
    public void        setUserId(Long userId) { this.userId = userId; }

    public String      getRole()      { return role; }
    public void        setRole(String role) { this.role = role; }

    public UserDetails getUserDetails() { return userDetails; }
    public void        setUserDetails(UserDetails userDetails) { this.userDetails = userDetails; }
}
