package com.team31.financetracker.user.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserEventConfig {

    public static final String USER_EVENTS_EXCHANGE = "user.events";
    public static final String TRANSACTION_EVENTS_EXCHANGE = "transaction.events";
    public static final String USER_DLX_EXCHANGE = "user.dlx";

    public static final String USER_TRANSACTION_SAGA_LISTENER_QUEUE = "user.transaction.saga-listener";
    public static final String USER_TRANSACTION_SAGA_LISTENER_DLQ = "user.transaction.saga-listener.dlq";

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange transactionEventsExchange() {
        return new TopicExchange(TRANSACTION_EVENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange userDlxExchange() {
        return new TopicExchange(USER_DLX_EXCHANGE);
    }

    @Bean
    public Queue userTransactionSagaListenerDlq() {
        return QueueBuilder.durable(USER_TRANSACTION_SAGA_LISTENER_DLQ).build();
    }

    @Bean
    public Binding userTransactionSagaListenerDlqBinding(
            TopicExchange userDlxExchange,
            Queue userTransactionSagaListenerDlq
    ) {
        return BindingBuilder.bind(userTransactionSagaListenerDlq)
                .to(userDlxExchange)
                .with(USER_TRANSACTION_SAGA_LISTENER_DLQ);
    }

    @Bean
    public Queue userTransactionSagaListenerQueue() {
        return QueueBuilder.durable(USER_TRANSACTION_SAGA_LISTENER_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_TRANSACTION_SAGA_LISTENER_DLQ)
                .build();
    }

    @Bean
    public Binding userTransactionSagaListenerBindingCompleted(
            TopicExchange transactionEventsExchange,
            Queue userTransactionSagaListenerQueue
    ) {
        return BindingBuilder.bind(userTransactionSagaListenerQueue)
                .to(transactionEventsExchange)
                .with("transaction.completed");
    }

    @Bean
    public Binding userTransactionSagaListenerBindingVoided(
            TopicExchange transactionEventsExchange,
            Queue userTransactionSagaListenerQueue
    ) {
        return BindingBuilder.bind(userTransactionSagaListenerQueue)
                .to(transactionEventsExchange)
                .with("transaction.voided");
    }
}
