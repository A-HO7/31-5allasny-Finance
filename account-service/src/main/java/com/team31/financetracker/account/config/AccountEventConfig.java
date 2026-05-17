package com.team31.financetracker.account.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.core.QueueBuilder;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AccountEventConfig {

    @Value("${rabbitmq.queues.transaction-saga-listener}")
    private String sagaListenerQueue;

    @Value("${rabbitmq.queues.transaction-saga-listener-dlq}")
    private String sagaListenerDlq;

    @Value("${rabbitmq.exchanges.account}")
    private String accountExchange;

    @Value("${rabbitmq.exchanges.transaction}")
    private String transactionExchange;

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
    public TopicExchange transactionEventsExchange() {
        return new TopicExchange(transactionExchange);
    }

    @Bean
    public Queue accountTransactionSagaListenerQueue() {
        return QueueBuilder.durable(sagaListenerQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", sagaListenerDlq)
                .build();
    }

    @Bean
    public Queue accountTransactionSagaListenerDlq() {
        return new Queue(sagaListenerDlq, true);
    }

    @Bean
    public Binding transactionApprovedBinding() {
        return BindingBuilder.bind(accountTransactionSagaListenerQueue())
                .to(transactionEventsExchange())
                .with(transactionApprovedRoutingKey);
    }

    @Bean
    public Binding transactionCompletedBinding() {
        return BindingBuilder.bind(accountTransactionSagaListenerQueue())
                .to(transactionEventsExchange())
                .with(transactionCompletedRoutingKey);
    }

    @Bean
    public Binding transactionVoidedBinding() {
        return BindingBuilder.bind(accountTransactionSagaListenerQueue())
                .to(transactionEventsExchange())
                .with(transactionVoidedRoutingKey);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
