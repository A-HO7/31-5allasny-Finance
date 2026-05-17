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

    @Value("${rabbitmq.queues.transaction-approved-listener}")
    private String transactionApprovedListenerQueue;

    @Value("${rabbitmq.queues.transaction-approved-listener-dlq}")
    private String transactionApprovedListenerDlq;

    @Value("${rabbitmq.queues.transaction-completed-listener}")
    private String transactionCompletedListenerQueue;

    @Value("${rabbitmq.queues.transaction-completed-listener-dlq}")
    private String transactionCompletedListenerDlq;

    @Value("${rabbitmq.queues.transaction-voided-listener}")
    private String transactionVoidedListenerQueue;

    @Value("${rabbitmq.queues.transaction-voided-listener-dlq}")
    private String transactionVoidedListenerDlq;

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
    public Queue transactionApprovedListenerQueue() {
        return queueWithDlq(transactionApprovedListenerQueue, transactionApprovedListenerDlq);
    }

    @Bean
    public Queue transactionApprovedListenerDlq() {
        return new Queue(transactionApprovedListenerDlq, true);
    }

    @Bean
    public Queue transactionCompletedListenerQueue() {
        return queueWithDlq(transactionCompletedListenerQueue, transactionCompletedListenerDlq);
    }

    @Bean
    public Queue transactionCompletedListenerDlq() {
        return new Queue(transactionCompletedListenerDlq, true);
    }

    @Bean
    public Queue transactionVoidedListenerQueue() {
        return queueWithDlq(transactionVoidedListenerQueue, transactionVoidedListenerDlq);
    }

    @Bean
    public Queue transactionVoidedListenerDlq() {
        return new Queue(transactionVoidedListenerDlq, true);
    }

    private Queue queueWithDlq(String queueName, String dlqName) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", dlqName);
        return new Queue(queueName, true, false, false, args);
    }

    @Bean
    public Binding transactionApprovedBinding() {
        return BindingBuilder.bind(transactionApprovedListenerQueue()).to(new TopicExchange(transactionExchange)).with(transactionApprovedRoutingKey);
    }

    @Bean
    public Binding transactionCompletedBinding() {
        return BindingBuilder.bind(transactionCompletedListenerQueue()).to(new TopicExchange(transactionExchange)).with(transactionCompletedRoutingKey);
    }

    @Bean
    public Binding transactionVoidedBinding() {
        return BindingBuilder.bind(transactionVoidedListenerQueue()).to(new TopicExchange(transactionExchange)).with(transactionVoidedRoutingKey);
    }
}
