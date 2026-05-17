package com.team31.financetracker.transaction.listener;

import com.team31.financetracker.contracts.events.UserDeactivatedEvent;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import com.team31.financetracker.transaction.service.CacheInvalidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionUserListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionUserListener.class);

    private final TransactionRepository transactionRepository;
    private final CacheInvalidationService cacheInvalidationService;

    public TransactionUserListener(TransactionRepository transactionRepository,
            CacheInvalidationService cacheInvalidationService) {
        this.transactionRepository = transactionRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @RabbitListener(queues = "transaction.user-listener")
    @Transactional
    public void onUserDeactivated(UserDeactivatedEvent event) {
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
    }
}
