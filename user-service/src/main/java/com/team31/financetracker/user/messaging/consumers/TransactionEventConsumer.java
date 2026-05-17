package com.team31.financetracker.user.messaging.consumers;

import com.team31.financetracker.contracts.events.TransactionCompletedEvent;
import com.team31.financetracker.contracts.events.TransactionVoidedEvent;
import com.team31.financetracker.user.entity.ProcessedEvent;
import com.team31.financetracker.user.repository.ProcessedEventRepository;
import com.team31.financetracker.user.repository.UserRepository;
import com.team31.financetracker.user.config.UserEventConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RabbitListener(queues = UserEventConfig.USER_TRANSACTION_SAGA_LISTENER_QUEUE)
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private static final Set<String> VOID_ROLLBACK_ELIGIBLE_STATUSES = Set.of(
            "COMPLETING",
            "REPORT_PENDING",
            "REPORT_FAILED",
            "REPORTED"
    );

    private final UserRepository userRepository;
    private final ProcessedEventRepository processedEventRepository;

    public TransactionEventConsumer(UserRepository userRepository,
                                    ProcessedEventRepository processedEventRepository) {
        this.userRepository = userRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @RabbitHandler
    @Transactional
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        String eventIdKey = "transaction.completed:" + event.transactionId();

        if (processedEventRepository.existsById(eventIdKey)) {
            log.info("Duplicate event skipped");
            return;
        }

        processedEventRepository.save(new ProcessedEvent(eventIdKey, LocalDateTime.now()));

        double amount = event.amount() != null ? event.amount() : 0.0;
        boolean isIncome = isIncome(event.type());
        userRepository.incrementStats(event.userId(), amount, isIncome);
    }

    @RabbitHandler
    @Transactional
    public void onTransactionVoided(TransactionVoidedEvent event) {
        String eventIdKey = "transaction.voided:" + event.transactionId();

        if (processedEventRepository.existsById(eventIdKey)) {
            log.info("Duplicate event skipped");
            return;
        }

        processedEventRepository.save(new ProcessedEvent(eventIdKey, LocalDateTime.now()));

        String previousStatus = event.previousStatus();
        if (previousStatus == null || !VOID_ROLLBACK_ELIGIBLE_STATUSES.contains(previousStatus)) {
            log.info("No rollback needed");
            return;
        }

        double amount = event.amount() != null ? event.amount() : 0.0;
        boolean isIncome = isIncome(event.type());
        userRepository.decrementStats(event.userId(), amount, isIncome);
    }

    private static boolean isIncome(String transactionType) {
        return transactionType != null && "INCOME".equalsIgnoreCase(transactionType.trim());
    }
}
