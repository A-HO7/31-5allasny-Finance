package com.team31.financetracker.user.config.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthContext {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final FilterChain filterChain;
    
    private String token;
    private String userEmail;
    private UserDetails userDetails;

    public AuthContext(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        this.request = request;
        this.response = response;
        this.filterChain = filterChain;
    }

    public HttpServletRequest getRequest() { return request; }
    public HttpServletResponse getResponse() { return response; }
    public FilterChain getFilterChain() { return filterChain; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public UserDetails getUserDetails() { return userDetails; }
    public void setUserDetails(UserDetails userDetails) { this.userDetails = userDetails; }
}
