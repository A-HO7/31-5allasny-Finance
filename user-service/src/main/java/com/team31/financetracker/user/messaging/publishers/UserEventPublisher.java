package com.team31.financetracker.user.messaging.publishers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team31.financetracker.contracts.events.UserDeactivatedEvent;
import com.team31.financetracker.contracts.events.UserRegisteredEvent;
import com.team31.financetracker.user.entity.OutboxEvent;
import com.team31.financetracker.user.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Enqueues user domain events on the outbox for reliable publish to {@code user.events}.
 */
@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    public static final String ROUTING_USER_REGISTERED = "user.registered";
    public static final String ROUTING_USER_DEACTIVATED = "user.deactivated";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public UserEventPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        enqueue(ROUTING_USER_REGISTERED, event);
        log.info("Enqueued {} for userId={}", ROUTING_USER_REGISTERED, event.userId());
    }

    public void publishUserDeactivated(UserDeactivatedEvent event) {
        enqueue(ROUTING_USER_DEACTIVATED, event);
        log.info("Enqueued {} for userId={}", ROUTING_USER_DEACTIVATED, event.userId());
    }

    private void enqueue(String routingKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent row = new OutboxEvent(
                    routingKey,
                    json,
                    OutboxEvent.Status.PENDING,
                    LocalDateTime.now()
            );
            outboxEventRepository.save(row);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event for routingKey=" + routingKey, e);
        }
    }
}
