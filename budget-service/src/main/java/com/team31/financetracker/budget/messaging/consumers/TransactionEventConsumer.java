package com.team31.financetracker.budget.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team31.financetracker.budget.messaging.publishers.BudgetEventPublisher;
import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.ProcessedEvent;
import com.team31.financetracker.budget.repository.BudgetRepository;
import com.team31.financetracker.budget.repository.ProcessedEventRepository;
import com.team31.financetracker.contracts.events.TransactionCompletedEvent;
import com.team31.financetracker.contracts.events.TransactionVoidedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final BudgetRepository budgetRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final BudgetEventPublisher budgetEventPublisher;
    private final ObjectMapper objectMapper;

    public TransactionEventConsumer(BudgetRepository budgetRepository,
                                    ProcessedEventRepository processedEventRepository,
                                    BudgetEventPublisher budgetEventPublisher,
                                    ObjectMapper objectMapper) {
        this.budgetRepository = budgetRepository;
        this.processedEventRepository = processedEventRepository;
        this.budgetEventPublisher = budgetEventPublisher;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "budget.transaction.saga-listener")
    @Transactional
    public void onTransactionEvent(Message message,
                                   @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        try {
            byte[] body = message.getBody();
            switch (routingKey) {
                case "transaction.completed" -> handleTransactionCompleted(body);
                case "transaction.voided"    -> handleTransactionVoided(body);
                default -> log.warn("Unhandled routing key '{}' on budget.transaction.saga-listener", routingKey);
            }
        } catch (Exception ex) {
            log.error("Failed to process message with routingKey='{}': {}", routingKey, ex.getMessage(), ex);
            throw new RuntimeException(ex); // re-throw so RabbitMQ routes to DLQ
        }
    }

    // ── transaction.completed ─────────────────────────────────────────────────

    private void handleTransactionCompleted(byte[] body) throws Exception {
        TransactionCompletedEvent event = parse(body, TransactionCompletedEvent.class);
        if (event == null) return;

        String transactionId = event.transactionId().toString();
        if (processedEventRepository.existsById(transactionId)) {
            log.info("transaction.completed for txnId={} already processed. Skipping.", transactionId);
            return; // Idempotency check
        }

        log.info("transaction.completed: txnId={} userId={} category={} amount={}",
                transactionId, event.userId(), event.category(), event.amount());

        int rowsAffected = budgetRepository.applyTransactionCompleted(event.userId(), event.category(), event.amount());

        if (rowsAffected > 0) {
            // Fetch the updated budget and publish
            Optional<Budget> budgetOpt = budgetRepository.findLatestBudgetForUserNative(event.userId(), event.category());
            budgetOpt.ifPresent(budgetEventPublisher::publishUsageUpdated);
        } else {
            log.info("transaction.completed: no active budget found for userId={} category={} — no-op",
                    event.userId(), event.category());
        }

        // Mark event as processed
        processedEventRepository.save(new ProcessedEvent(transactionId));
    }

    // ── transaction.voided ────────────────────────────────────────────────────

    private void handleTransactionVoided(byte[] body) throws Exception {
        TransactionVoidedEvent event = parse(body, TransactionVoidedEvent.class);
        if (event == null) return;

        String transactionId = event.transactionId().toString() + "-voided";
        if (processedEventRepository.existsById(transactionId)) {
            log.info("transaction.voided for txnId={} already processed. Skipping.", transactionId);
            return; // Idempotency check
        }

        log.info("transaction.voided: txnId={} userId={} category={} amount={} previousStatus={}",
                event.transactionId(), event.userId(), event.category(), event.amount(), event.previousStatus());

        // Only reverse if the budget WAS incremented in the forward path
        String prev = event.previousStatus() != null ? event.previousStatus().toUpperCase() : "";
        if ("COMPLETING".equals(prev) || "REPORT_PENDING".equals(prev) || "REPORT_FAILED".equals(prev)) {
            int rowsAffected = budgetRepository.applyTransactionVoided(event.userId(), event.category(), event.amount());

            if (rowsAffected > 0) {
                // Fetch the updated budget and publish
                Optional<Budget> budgetOpt = budgetRepository.findLatestBudgetForUserNative(event.userId(), event.category());
                budgetOpt.ifPresent(budgetEventPublisher::publishUsageUpdated);
            }
        } else {
            log.info("transaction.voided: previousStatus '{}' means budget was never incremented — no-op", prev);
        }

        // Mark event as processed
        processedEventRepository.save(new ProcessedEvent(transactionId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> T parse(byte[] body, Class<T> type) throws Exception {
        if (body == null || body.length == 0) return null;
        return objectMapper.readValue(body, type);
    }
}
