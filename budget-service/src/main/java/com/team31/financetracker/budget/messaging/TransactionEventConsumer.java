package com.team31.financetracker.budget.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.BudgetStatus;
import com.team31.financetracker.budget.model.Category;
import com.team31.financetracker.budget.repository.BudgetRepository;
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
    private final BudgetEventPublisher budgetEventPublisher;
    private final ObjectMapper objectMapper;

    public TransactionEventConsumer(BudgetRepository budgetRepository,
                                    BudgetEventPublisher budgetEventPublisher,
                                    ObjectMapper objectMapper) {
        this.budgetRepository = budgetRepository;
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
        if (event == null) {
            log.warn("transaction.completed: null payload — skipping");
            return;
        }

        log.info("transaction.completed: txnId={} userId={} category={} amount={}",
                event.transactionId(), event.userId(), event.category(), event.amount());

        Category category = resolveCategory(event.category());
        if (category == null) {
            log.warn("transaction.completed: unknown category '{}' — skipping", event.category());
            return;
        }

        Optional<Budget> budgetOpt = budgetRepository.findActiveBudgetForUserNative(event.userId(), category);
        if (budgetOpt.isEmpty()) {
            log.info("transaction.completed: no active budget found for userId={} category={} — skipping",
                    event.userId(), category);
            return;
        }

        Budget budget = budgetOpt.get();
        double newSpent = budget.getSpentAmount() + event.amount();
        budget.setSpentAmount(newSpent);

        // Transition status: ACTIVE → EXCEEDED if over limit
        if (newSpent > budget.getAmount() && budget.getStatus() == BudgetStatus.ACTIVE) {
            budget.setStatus(BudgetStatus.EXCEEDED);
            log.info("Budget budgetId={} transitioned ACTIVE→EXCEEDED (spent={} limit={})",
                    budget.getId(), newSpent, budget.getAmount());
        }

        budgetRepository.save(budget);
        budgetEventPublisher.publishUsageUpdated(budget);
    }

    // ── transaction.voided ────────────────────────────────────────────────────

    private void handleTransactionVoided(byte[] body) throws Exception {
        TransactionVoidedEvent event = parse(body, TransactionVoidedEvent.class);
        if (event == null) {
            log.warn("transaction.voided: null payload — skipping");
            return;
        }

        log.info("transaction.voided: txnId={} userId={} category={} amount={}",
                event.transactionId(), event.userId(), event.category(), event.amount());

        Category category = resolveCategory(event.category());
        if (category == null) {
            log.warn("transaction.voided: unknown category '{}' — skipping", event.category());
            return;
        }

        Optional<Budget> budgetOpt = budgetRepository.findActiveBudgetForUserNative(event.userId(), category);
        // Also check exceeded budgets in case it was exceeded before voiding
        if (budgetOpt.isEmpty()) {
            budgetOpt = budgetRepository
                    .findAll()
                    .stream()
                    .filter(b -> b.getUserId().equals(event.userId())
                            && b.getCategory() == category
                            && b.getStatus() == BudgetStatus.EXCEEDED)
                    .findFirst();
        }

        if (budgetOpt.isEmpty()) {
            log.info("transaction.voided: no matching budget found for userId={} category={} — skipping",
                    event.userId(), category);
            return;
        }

        Budget budget = budgetOpt.get();
        double newSpent = Math.max(0.0, budget.getSpentAmount() - event.amount());
        budget.setSpentAmount(newSpent);

        // Roll back status: EXCEEDED → ACTIVE if now within limit
        if (newSpent <= budget.getAmount() && budget.getStatus() == BudgetStatus.EXCEEDED) {
            budget.setStatus(BudgetStatus.ACTIVE);
            log.info("Budget budgetId={} rolled back EXCEEDED→ACTIVE (spent={} limit={})",
                    budget.getId(), newSpent, budget.getAmount());
        }

        budgetRepository.save(budget);
        budgetEventPublisher.publishUsageUpdated(budget);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Category resolveCategory(String categoryStr) {
        if (categoryStr == null) return null;
        try {
            return Category.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private <T> T parse(byte[] body, Class<T> type) throws Exception {
        if (body == null || body.length == 0) return null;
        return objectMapper.readValue(body, type);
    }
}
