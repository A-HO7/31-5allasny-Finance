package com.team31.financetracker.transaction.listener;

import com.team31.financetracker.contracts.events.UserDeactivatedEvent;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import com.team31.financetracker.transaction.service.CacheInvalidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TransactionUserListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionUserListener.class);

    private final TransactionRepository transactionRepository;
    private final CacheInvalidationService cacheInvalidationService;

    private final ObjectMapper objectMapper;

    public TransactionUserListener(TransactionRepository transactionRepository,
            CacheInvalidationService cacheInvalidationService,
            ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.cacheInvalidationService = cacheInvalidationService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "transaction.user-listener")
    @Transactional
    public void onUserEvent(org.springframework.amqp.core.Message message,
            @org.springframework.messaging.handler.annotation.Header(org.springframework.amqp.support.AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        
        byte[] body = message.getBody();
        if (body == null || body.length == 0) return;
        String text = new String(body, java.nio.charset.StandardCharsets.UTF_8).trim();

        try {
            if ("user.deactivated".equals(routingKey)) {
                UserDeactivatedEvent event = objectMapper.readValue(text, UserDeactivatedEvent.class);
                log.info("Received user.deactivated for userId={}", event.userId());

                int rows = transactionRepository.voidPendingByUserId(event.userId());
                if (rows == 0) {
                    log.warn("No PENDING transactions to void for userId={}", event.userId());
                    return;
                }
                log.info("Voided {} PENDING transactions for userId={}", rows, event.userId());

                // Invalidate caches after bulk mutation
                cacheInvalidationService.evictF1();
                cacheInvalidationService.evictF5();
                cacheInvalidationService.evictF6();
            } else if ("user.registered".equals(routingKey)) {
                com.team31.financetracker.contracts.events.UserRegisteredEvent event = objectMapper.readValue(text, com.team31.financetracker.contracts.events.UserRegisteredEvent.class);
                log.info("Received user.registered for userId={}", event.userId());
                // No specific action required for transaction-service on user registration
            } else {
                log.warn("Unhandled routing key {} on transaction.user-listener", routingKey);
            }
        } catch (Exception ex) {
            log.error("Failed to handle user event message rk={}", routingKey, ex);
            throw new RuntimeException(ex);
        }
    }
}
