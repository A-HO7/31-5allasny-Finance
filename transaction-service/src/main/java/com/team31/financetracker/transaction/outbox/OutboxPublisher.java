package com.team31.financetracker.transaction.outbox;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void drain() {
        outboxRepository.findByPublishedFalseOrderByCreatedAtAsc()
                .forEach(event -> {
                    rabbitTemplate.convertAndSend(
                            event.getExchange(),
                            event.getRoutingKey(),
                            event.getPayload());
                    event.setPublished(true);
                    outboxRepository.save(event);
                });
    }
}