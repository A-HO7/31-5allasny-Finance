package com.team31.financetracker.user.config;

import com.team31.financetracker.user.config.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Custom entry point: return 401 instead of redirecting to login page
        AuthenticationEntryPoint entryPoint = (request, response, authException) -> {
            response.setStatus(401);
            response.getWriter().write("Unauthorized");
        };
        // Custom access denied handler: return 403 with clean message
        AccessDeniedHandler accessDeniedHandler = (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.getWriter().write("Forbidden");
        };

        http
                // 1. Disable CSRF (required for POST requests in REST APIs)
                .csrf(csrf -> csrf.disable())

                // 2. Set session management to stateless (JWTs don't use server sessions)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. Configure endpoint security rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/users/health",
                                "/actuator/**",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // 4. Custom exception handlers so Spring doesn't swallow our 409/400 responses
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // 5. Register our custom JWT Filter (which uses the Chain of Responsibility)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

