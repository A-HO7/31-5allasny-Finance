package com.team31.financetracker.account.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AccountEventConfig {

    @Value("${rabbitmq.exchanges.account}")
    private String accountExchange;

    @Value("${rabbitmq.exchanges.transaction}")
    private String transactionExchange;

    @Value("${rabbitmq.queues.transaction-saga-listener}")
    private String transactionSagaListenerQueue;

    @Value("${rabbitmq.queues.transaction-saga-listener-dlq}")
    private String transactionSagaListenerDlq;

    @Value("${rabbitmq.routing-keys.transaction-approved}")
    private String transactionApprovedRoutingKey;

    @Value("${rabbitmq.routing-keys.transaction-completed}")
    private String transactionCompletedRoutingKey;

    @Value("${rabbitmq.routing-keys.transaction-voided}")
    private String transactionVoidedRoutingKey;

    @Bean
    public TopicExchange accountEventsExchange() {
        return new TopicExchange(accountExchange);
    }

    @Bean
    public Queue transactionSagaListenerQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", transactionSagaListenerDlq);
        return new Queue(transactionSagaListenerQueue, true, false, false, args);
    }

    @Bean
    public Queue transactionSagaListenerDlq() {
        return new Queue(transactionSagaListenerDlq, true);
    }

    @Bean
    public Binding transactionApprovedBinding() {
        return BindingBuilder.bind(transactionSagaListenerQueue()).to(new TopicExchange(transactionExchange)).with(transactionApprovedRoutingKey);
    }

    @Bean
    public Binding transactionCompletedBinding() {
        return BindingBuilder.bind(transactionSagaListenerQueue()).to(new TopicExchange(transactionExchange)).with(transactionCompletedRoutingKey);
    }

    @Bean
    public Binding transactionVoidedBinding() {
        return BindingBuilder.bind(transactionSagaListenerQueue()).to(new TopicExchange(transactionExchange)).with(transactionVoidedRoutingKey);
    }
}
