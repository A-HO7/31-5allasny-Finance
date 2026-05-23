package com.team31.financetracker.user.messaging.consumers;

import com.team31.financetracker.contracts.events.TransactionCompletedEvent;
import com.team31.financetracker.contracts.events.TransactionVoidedEvent;
import com.team31.financetracker.user.config.UserEventConfig;
import com.team31.financetracker.user.entity.ProcessedEvent;
import com.team31.financetracker.user.repository.ProcessedEventRepository;
import com.team31.financetracker.user.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
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
    private final Counter rabbitmqConsumedCounter;

    public TransactionEventConsumer(UserRepository userRepository,
                                    ProcessedEventRepository processedEventRepository,
                                    Counter rabbitmqConsumedCounter) {
        this.userRepository = userRepository;
        this.processedEventRepository = processedEventRepository;
        this.rabbitmqConsumedCounter = rabbitmqConsumedCounter;
    }

    @RabbitHandler
    @Transactional
    public void onTransactionCompleted(TransactionCompletedEvent event, @Headers Map<String, Object> headers) {
        String correlationId = (String) headers.get("X-Correlation-ID");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        MDC.put("routingKey", UserEventConfig.RK_TRANSACTION_COMPLETED);
        try {
            String eventIdKey = "transaction.completed:" + event.transactionId();

            if (processedEventRepository.existsById(eventIdKey)) {
                log.info("Duplicate event skipped");
                return;
            }

            processedEventRepository.save(new ProcessedEvent(eventIdKey, LocalDateTime.now()));

            double amount = event.amount() != null ? event.amount() : 0.0;
            boolean isIncome = isIncome(event.type());
            userRepository.incrementStats(event.userId(), amount, isIncome);
            rabbitmqConsumedCounter.increment();
        } finally {
            MDC.clear();
        }
    }

    @RabbitHandler
    @Transactional
    public void onTransactionVoided(TransactionVoidedEvent event, @Headers Map<String, Object> headers) {
        String correlationId = (String) headers.get("X-Correlation-ID");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        MDC.put("routingKey", UserEventConfig.RK_TRANSACTION_VOIDED);
        try {
            String eventIdKey = "transaction.voided:" + event.transactionId();

            if (processedEventRepository.existsById(eventIdKey)) {
                log.info("Duplicate event skipped");
                return;
            }

            processedEventRepository.save(new ProcessedEvent(eventIdKey, LocalDateTime.now()));

            String previousStatus = event.previousStatus();
            if (previousStatus != null && VOID_ROLLBACK_ELIGIBLE_STATUSES.contains(previousStatus)) {
                double amount = event.amount() != null ? event.amount() : 0.0;
                boolean isIncome = isIncome(event.type());
                userRepository.decrementStats(event.userId(), amount, isIncome);
            } else {
                log.info("No rollback needed");
            }
            rabbitmqConsumedCounter.increment();
        } finally {
            MDC.clear();
        }
    }

    private static boolean isIncome(String transactionType) {
        return transactionType != null && "INCOME".equalsIgnoreCase(transactionType.trim());
    }
}
