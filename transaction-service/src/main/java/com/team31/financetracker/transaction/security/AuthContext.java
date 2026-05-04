package com.team31.financetracker.transaction.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class AuthContext {

    private final HttpServletRequest request;
    private String  token;
    private Long    userId;
    private String  role;
    private String  email;
    private String  requiredRole; // set by SecurityConfig per endpoint if needed

    public AuthContext(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletRequest getRequest()  { return request; }
    public String  getToken()               { return token; }
    public void    setToken(String t)       { this.token = t; }
    public Long    getUserId()              { return userId; }
    public void    setUserId(Long id)       { this.userId = id; }
    public String  getRole()               { return role; }
    public void    setRole(String r)        { this.role = r; }
    public String  getEmail()              { return email; }
    public void    setEmail(String e)       { this.email = e; }
    public String  getRequiredRole()       { return requiredRole; }
    public void    setRequiredRole(String r){ this.requiredRole = r; }
}
