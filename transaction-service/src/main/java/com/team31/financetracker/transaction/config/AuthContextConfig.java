package com.team31.financetracker.transaction.config;

import com.team31.financetracker.transaction.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.context.WebApplicationContext;

@Configuration
public class AuthContextConfig {
    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS)
    public AuthContext authContext(ObjectProvider<HttpServletRequest> requestProvider) {
        HttpServletRequest request = requestProvider.getIfAvailable();
        return new AuthContext(request);
    }
}
