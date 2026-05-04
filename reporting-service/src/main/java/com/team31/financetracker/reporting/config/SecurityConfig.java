package com.team31.financetracker.reporting.config;

import com.team31.financetracker.reporting.config.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the reporting-service.
 *
 * - Stateless sessions (JWTs; no HttpSession created).
 * - CSRF disabled (REST API, no browser forms).
 * - Health endpoint is public; all other requests require authentication.
 * - Authentication is enforced by JwtAuthenticationFilter (DP-3 CoR chain).
 *
 * Note: No PasswordEncoder bean needed — reporting-service never hashes passwords.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF — not needed for stateless REST APIs
            .csrf(csrf -> csrf.disable())

            // 2. Stateless session — no server-side session for JWT auth
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 3. Public vs protected endpoints
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/reports/health").permitAll()
                    .anyRequest().authenticated()
            )

            // 4. Insert our custom JWT filter (which runs the CoR chain)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
