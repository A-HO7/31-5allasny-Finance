package com.team31.financetracker.transaction.messaging.consumers;

import com.team31.financetracker.contracts.events.AccountStatusChangedEvent;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import com.team31.financetracker.transaction.service.CacheInvalidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountStatusConsumer.class);

    private final TransactionRepository transactionRepository;
    private final CacheInvalidationService cacheInvalidationService;

    public AccountStatusConsumer(TransactionRepository transactionRepository,
            CacheInvalidationService cacheInvalidationService) {
        this.transactionRepository = transactionRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @RabbitListener(queues = "transaction.account-listener")
    @Transactional
    public void onAccountStatusChanged(AccountStatusChangedEvent event) {
        log.info("Received account.status-changed accountId={} newStatus={}", event.accountId(), event.newStatus());

        String newStatus = event.newStatus();
        if ("FROZEN".equalsIgnoreCase(newStatus) || "CLOSED".equalsIgnoreCase(newStatus)) {
            int rows = transactionRepository.voidPendingByAccountId(event.accountId());
            log.info("Voided {} PENDING transactions for accountId={}", rows, event.accountId());

            cacheInvalidationService.evictF1();
            cacheInvalidationService.evictF5();
            cacheInvalidationService.evictF6();
        } else {
            log.info("Account status change to {} does not require voiding transactions", newStatus);
        }
    }
}
