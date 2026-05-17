package com.team31.financetracker.reporting.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates the SLF4J MDC with observability keys for every
 * inbound HTTP request, so every log line emitted during that request carries the
 * correct correlationId without manual plumbing in each controller.
 *
 * Keys set:
 *   - correlationId  — taken from X-Correlation-ID header (set by api-gateway) or
 *                      generated as a random UUID if absent (handles direct calls)
 *
 * All MDC keys are cleared in the finally block to prevent them leaking into
 * thread-pool reuse for subsequent requests.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION    = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION, correlationId);

        // Echo the correlation ID back to the caller so the gateway/client can trace
        response.setHeader(CORRELATION_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // MUST clear to prevent MDC leaking into next request on the same thread
            MDC.remove(MDC_CORRELATION);
        }
    }
}
