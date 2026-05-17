package com.team31.financetracker.transaction.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * M3 (S3-READ-DB): Feign interceptor to propagate tracing/auth headers.
 * Forwards X-Correlation-ID, X-User-Id, and X-User-Role downstream.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    
                    String correlationId = request.getHeader("X-Correlation-ID");
                    if (correlationId != null) {
                        template.header("X-Correlation-ID", correlationId);
                    }
                    
                    String userId = request.getHeader("X-User-Id");
                    if (userId != null) {
                        template.header("X-User-Id", userId);
                    }
                    
                    String userRole = request.getHeader("X-User-Role");
                    if (userRole != null) {
                        template.header("X-User-Role", userRole);
                    }
                }
            }
        };
    }
}
