package com.team31.financetracker.transaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // Exchanges (source services)
    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange("user.events");
    }

    @Bean
    public TopicExchange accountEventsExchange() {
        return new TopicExchange("account.events");
    }

    @Bean
    public TopicExchange reportEventsExchange() {
        return new TopicExchange("report.events");
    }

    @Bean
    public TopicExchange transactionDlx() {
        return new TopicExchange("transaction.dlx");
    }

    // Queues + DLQs
    @Bean
    public Queue transactionUserListenerQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "transaction.dlx");
        return new Queue("transaction.user-listener", true, false, false, args);
    }

    @Bean
    public Queue transactionUserListenerDlq() {
        return new Queue("transaction.user-listener.dlq");
    }

    @Bean
    public Queue transactionAccountListenerQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "transaction.dlx");
        return new Queue("transaction.account-listener", true, false, false, args);
    }

    @Bean
    public Queue transactionAccountListenerDlq() {
        return new Queue("transaction.account-listener.dlq");
    }

    @Bean
    public Queue transactionSagaFeedbackQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "transaction.dlx");
        return new Queue("transaction.saga-feedback", true, false, false, args);
    }

    @Bean
    public Queue transactionSagaFeedbackDlq() {
        return new Queue("transaction.saga-feedback.dlq");
    }

    // Bindings
    @Bean
    public Binding bindUserListener(Queue transactionUserListenerQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(transactionUserListenerQueue).to(userEventsExchange).with("user.deactivated");
    }

    @Bean
    public Binding bindAccountListener(Queue transactionAccountListenerQueue, TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(transactionAccountListenerQueue).to(accountEventsExchange)
                .with("account.status-changed");
    }

    @Bean
    public Binding bindSagaFeedback(Queue transactionSagaFeedbackQueue, TopicExchange reportEventsExchange) {
        // Use wildcard to receive all report.* routing keys
        // (initiated/completed/failed/reverted)
        return BindingBuilder.bind(transactionSagaFeedbackQueue).to(reportEventsExchange).with("report.#");
    }

    // DLQ bindings -> send all dead letters into the transaction.dlx exchange
    @Bean
    public Binding bindUserDlq(Queue transactionUserListenerDlq, TopicExchange transactionDlx) {
        return BindingBuilder.bind(transactionUserListenerDlq).to(transactionDlx).with("#");
    }

    @Bean
    public Binding bindAccountDlq(Queue transactionAccountListenerDlq, TopicExchange transactionDlx) {
        return BindingBuilder.bind(transactionAccountListenerDlq).to(transactionDlx).with("#");
    }

    @Bean
    public Binding bindSagaDlq(Queue transactionSagaFeedbackDlq, TopicExchange transactionDlx) {
        return BindingBuilder.bind(transactionSagaFeedbackDlq).to(transactionDlx).with("#");
    }

    // JSON message converter for RabbitTemplate
    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // Spring Boot will auto-configure RabbitTemplate and pick up the
    // MessageConverter bean above.
}
