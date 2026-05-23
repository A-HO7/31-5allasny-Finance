package com.team31.financetracker.apigateway.filter;

import com.team31.financetracker.apigateway.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

/**
 * M3 §9.3 — Reactive JWT Global Filter for the API Gateway.
 * <p>
 * Runs before routing (order = -1). Validates JWT via {@link JwtService} on a
 * boundedElastic scheduler thread so JJWT's blocking signature check never pins
 * the Reactor event loop. Forwards X-User-Id, X-User-Role, X-Correlation-ID to
 * downstream services via the immutable ServerHttpRequest mutator pattern.
 */
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public JwtGatewayFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // Pass-through: register and login do not yet have a JWT
        if (path.startsWith("/api/auth/")) {
            return forwardWithCorrelationId(exchange, chain);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // Wrap blocking JJWT validation on a non-event-loop thread (M3 requirement)
        return Mono.fromCallable(() -> jwtService.validateToken(token))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(claims -> {
                    String userId        = String.valueOf(claims.get("uid"));
                    String role          = String.valueOf(claims.get("role"));
                    String correlationId = resolveCorrelationId(exchange);

                    // ServerHttpRequest is immutable — use the mutator pattern
                    ServerWebExchange mutated = exchange.mutate()
                            .request(r -> r
                                    .header("X-User-Id", userId)
                                    .header("X-User-Role", role)
                                    .header("X-Correlation-ID", correlationId))
                            .build();

                    return chain.filter(mutated);
                })
                .onErrorResume(e -> {
                    // Do NOT throw on auth failure — reactive convention is to set status + complete
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
}
