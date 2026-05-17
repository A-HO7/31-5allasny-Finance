package com.team31.financetracker.user.messaging;

import com.team31.financetracker.user.config.UserEventConfig;
import com.team31.financetracker.user.entity.OutboxEvent;
import com.team31.financetracker.user.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

@Component
public class OutboxScheduler {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxScheduler(RabbitTemplate rabbitTemplate,
                           OutboxEventRepository outboxEventRepository,
                           ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    public void processPendingOutboxEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        for (OutboxEvent event : pending) {
            try {
                Object body = objectMapper.readValue(event.getPayload(), Object.class);
                rabbitTemplate.convertAndSend(
                        UserEventConfig.USER_EVENTS_EXCHANGE,
                        event.getRoutingKey(),
                        body
                );
                event.setStatus(OutboxEvent.Status.SENT);
                outboxEventRepository.save(event);
            } catch (Exception ignored) {
                // Per-event isolation (JSON, broker, persistence): failures leave row PENDING for retry.
            }
        }
    }
}
