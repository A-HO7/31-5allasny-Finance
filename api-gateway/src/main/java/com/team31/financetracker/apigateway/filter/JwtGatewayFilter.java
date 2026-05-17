package com.team31.financetracker.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    @Value("${JWT_SECRET:StandardSecretKeyForACLProject2026_32Bytes}")
    private String jwtSecret;

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        if (path.startsWith("/api/auth/")) {
            return forwardWithCorrelationId(exchange, chain);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        return Mono.fromCallable(() -> extractClaims(token))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(claims -> {
                    String userId = String.valueOf(claims.get("uid"));
                    String role = String.valueOf(claims.get("role"));
                    String correlationId = resolveCorrelationId(exchange);

                    ServerWebExchange mutated = exchange.mutate()
                            .request(r -> r
                                    .header("X-User-Id", userId)
                                    .header("X-User-Role", role)
                                    .header("X-Correlation-ID", correlationId))
                            .build();

                    return chain.filter(mutated);
                })
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    private Mono<Void> forwardWithCorrelationId(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange);
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header("X-Correlation-ID", correlationId))
                .build();
        return chain.filter(mutated);
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private Claims extractClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
