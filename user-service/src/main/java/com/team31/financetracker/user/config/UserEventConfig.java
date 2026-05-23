package com.team31.financetracker.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class UserEventConfig {

    public static final String USER_EVENTS_EXCHANGE = "user.events";
    public static final String TRANSACTION_EVENTS_EXCHANGE = "transaction.events";
    public static final String USER_DLX_EXCHANGE = "user.dlx";

    public static final String USER_TRANSACTION_SAGA_LISTENER_QUEUE = "user.transaction.saga-listener";
    public static final String USER_TRANSACTION_SAGA_LISTENER_DLQ = "user.transaction.saga-listener.dlq";

    public static final String RK_USER_REGISTERED = "user.registered";
    public static final String RK_USER_DEACTIVATED = "user.deactivated";
    public static final String RK_TRANSACTION_COMPLETED = "transaction.completed";
    public static final String RK_TRANSACTION_VOIDED = "transaction.voided";

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

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
                .with(RK_TRANSACTION_COMPLETED);
    }

    @Bean
    public Binding userTransactionSagaListenerBindingVoided(
            TopicExchange transactionEventsExchange,
            Queue userTransactionSagaListenerQueue
    ) {
        return BindingBuilder.bind(userTransactionSagaListenerQueue)
                .to(transactionEventsExchange)
                .with(RK_TRANSACTION_VOIDED);
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
