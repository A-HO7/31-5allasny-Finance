package com.team31.financetracker.budget.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class BudgetEventConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // ── Exchanges ──────────────────────────────────────────────────────────────

    /** Exchange that budget-service publishes TO (e.g. budget.usage-updated) */
    @Bean
    public TopicExchange budgetEventsExchange() {
        return new TopicExchange("budget.events");
    }

    /** Exchange that budget-service consumes FROM (transaction.completed / voided) */
    @Bean
    public TopicExchange transactionEventsExchange() {
        return new TopicExchange("transaction.events");
    }

    /** Dead-letter exchange — receives messages rejected/expired from budget queues */
    @Bean
    public TopicExchange budgetDlx() {
        return new TopicExchange("budget.dlx");
    }

    // ── Queues ─────────────────────────────────────────────────────────────────

    @Bean
    public Queue budgetTransactionSagaListenerQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "budget.dlx");
        return new Queue("budget.transaction.saga-listener", true, false, false, args);
    }

    @Bean
    public Queue budgetTransactionSagaListenerDlq() {
        return new Queue("budget.transaction.saga-listener.dlq", true);
    }

    // ── Bindings ───────────────────────────────────────────────────────────────

    /** budget.transaction.saga-listener receives transaction.completed events */
    @Bean
    public Binding bindSagaListenerCompleted(Queue budgetTransactionSagaListenerQueue,
                                             TopicExchange transactionEventsExchange) {
        return BindingBuilder.bind(budgetTransactionSagaListenerQueue)
                .to(transactionEventsExchange)
                .with("transaction.completed");
    }

    /** budget.transaction.saga-listener also receives transaction.voided events */
    @Bean
    public Binding bindSagaListenerVoided(Queue budgetTransactionSagaListenerQueue,
                                          TopicExchange transactionEventsExchange) {
        return BindingBuilder.bind(budgetTransactionSagaListenerQueue)
                .to(transactionEventsExchange)
                .with("transaction.voided");
    }

    /** DLQ catches all dead letters from budget.dlx */
    @Bean
    public Binding bindSagaListenerDlq(Queue budgetTransactionSagaListenerDlq,
                                       TopicExchange budgetDlx) {
        return BindingBuilder.bind(budgetTransactionSagaListenerDlq)
                .to(budgetDlx)
                .with("#");
    }

    // ── Message converter ──────────────────────────────────────────────────────

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
