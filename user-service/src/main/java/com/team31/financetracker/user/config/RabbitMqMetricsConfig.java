package com.team31.financetracker.user.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqMetricsConfig {

    /**
     * Exposes {@code rabbitmq_consumed_total{service="user-service"}} for Prometheus/Grafana.
     * Increment via {@link #incrementConsumed(MeterRegistry)} when message consumers are wired.
     */
    @Bean
    public Counter rabbitmqConsumedCounter(MeterRegistry registry) {
        return Counter.builder("rabbitmq.consumed")
                .description("RabbitMQ messages consumed by user-service")
                .tag("service", "user-service")
                .register(registry);
    }

    public static void incrementConsumed(MeterRegistry registry) {
        registry.counter("rabbitmq.consumed", "service", "user-service").increment();
    }
}
