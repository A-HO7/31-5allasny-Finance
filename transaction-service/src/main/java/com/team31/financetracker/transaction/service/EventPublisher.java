package com.team31.financetracker.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String exchange, String routingKey, Object payload) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(payload);
            MessageProperties properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            rabbitTemplate.send(exchange, routingKey, new Message(body, properties));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}