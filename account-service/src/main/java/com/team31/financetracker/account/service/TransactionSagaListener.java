package com.team31.financetracker.account.service;

import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.ProcessedEvent;
import com.team31.financetracker.account.repository.AccountRepository;
import com.team31.financetracker.account.repository.ProcessedEventRepository;
import com.team31.financetracker.contracts.events.AccountStatsUpdatedEvent;
import com.team31.financetracker.contracts.events.TransactionApprovedEvent;
import com.team31.financetracker.contracts.events.TransactionCompletedEvent;
import com.team31.financetracker.contracts.events.TransactionVoidedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionSagaListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionSagaListener.class);

    private final AccountRepository accountRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final AccountEventPublisher accountEventPublisher;

    public TransactionSagaListener(AccountRepository accountRepository, ProcessedEventRepository processedEventRepository, AccountEventPublisher accountEventPublisher) {
        this.accountRepository = accountRepository;
        this.processedEventRepository = processedEventRepository;
        this.accountEventPublisher = accountEventPublisher;
    }

    private boolean isEventProcessed(String eventId) {
        return processedEventRepository.findById(eventId).isPresent();
    }

    private void markEventAsProcessed(String eventId) {
        processedEventRepository.save(new ProcessedEvent(eventId));
    }

    @RabbitListener(queues = "${rabbitmq.queues.transaction-approved-listener}")
    @Transactional
    public void onTransactionApproved(TransactionApprovedEvent event, @Header("amqp_receivedRoutingKey") String routingKey, @Header(value = "x-correlation-id", required = false) String correlationId) {
        String eventId = eventId(event.transactionId(), "APPROVED");
        try {
            putMdc(routingKey, correlationId, event.accountId());
            if (isEventProcessed(eventId)) {
                LOGGER.warn("Event {} already processed.", eventId);
                return;
            }

            LOGGER.info("Processing transaction.approved event for account {}", event.accountId());

            String type = requireType(event.type(), eventId);
            Double amount = requireAmount(event.amount(), eventId);

            if ("EXPENSE".equalsIgnoreCase(type)) {
                updateBalance(event.accountId(), -amount, eventId);
            } else if ("INCOME".equalsIgnoreCase(type)) {
                updateBalance(event.accountId(), amount, eventId);
            } else if ("TRANSFER".equalsIgnoreCase(type)) {
                updateBalance(event.accountId(), -amount, eventId);
                updateBalance(event.toAccountId(), amount, eventId);
            } else {
                throw new IllegalArgumentException("Unsupported transaction type for event " + eventId + ": " + type);
            }

            LOGGER.info("Updated balance for account {}", event.accountId());
            markEventAsProcessed(eventId);
        } finally {
            MDC.clear();
        }
    }

    @RabbitListener(queues = "${rabbitmq.queues.transaction-completed-listener}")
    @Transactional
    public void onTransactionCompleted(TransactionCompletedEvent event, @Header("amqp_receivedRoutingKey") String routingKey, @Header(value = "x-correlation-id", required = false) String correlationId) {
        String eventId = eventId(event.transactionId(), "COMPLETED");
        try {
            putMdc(routingKey, correlationId, event.accountId());
            if (isEventProcessed(eventId)) {
                LOGGER.warn("Event {} already processed.", eventId);
                return;
            }

            LOGGER.info("Processing transaction.completed event for account {}", event.accountId());
            ensureAccountUpdated(accountRepository.incrementTotalTransactions(event.accountId()), event.accountId(), eventId);
            ensureAccountUpdated(
                    accountRepository.updateLastTransactionDate(
                            event.accountId(),
                            event.completedAt() != null ? event.completedAt() : LocalDateTime.now()
                    ),
                    event.accountId(),
                    eventId
            );
            LOGGER.info("Updated transaction stats for account {}", event.accountId());

            Account account = accountRepository.findById(event.accountId()).orElseThrow(
                    () -> new IllegalStateException("Account not found for event " + eventId)
            );
            accountEventPublisher.publishAccountStatsUpdatedEvent(
                    new AccountStatsUpdatedEvent(account.getId(), 1, 0.0)
            );

            markEventAsProcessed(eventId);
        } finally {
            MDC.clear();
        }
    }

    @RabbitListener(queues = "${rabbitmq.queues.transaction-voided-listener}")
    @Transactional
    public void onTransactionVoided(TransactionVoidedEvent event, @Header("amqp_receivedRoutingKey") String routingKey, @Header(value = "x-correlation-id", required = false) String correlationId) {
        String eventId = eventId(event.transactionId(), "VOIDED");
        try {
            putMdc(routingKey, correlationId, event.accountId());
            if (isEventProcessed(eventId)) {
                LOGGER.warn("Event {} already processed.", eventId);
                return;
            }

            LOGGER.info("Processing transaction.voided event for account {}", event.accountId());

            String previousStatus = event.previousStatus();
            if ("APPROVED".equalsIgnoreCase(previousStatus) || "COMPLETING".equalsIgnoreCase(previousStatus) || "REPORT_PENDING".equalsIgnoreCase(previousStatus) || "REPORT_FAILED".equalsIgnoreCase(previousStatus)) {
                String type = requireType(event.type(), eventId);
                Double amount = requireAmount(event.amount(), eventId);

                if ("EXPENSE".equalsIgnoreCase(type)) {
                    updateBalance(event.accountId(), amount, eventId);
                } else if ("INCOME".equalsIgnoreCase(type)) {
                    updateBalance(event.accountId(), -amount, eventId);
                } else if ("TRANSFER".equalsIgnoreCase(type)) {
                    updateBalance(event.accountId(), amount, eventId);
                    updateBalance(event.toAccountId(), -amount, eventId);
                } else {
                    throw new IllegalArgumentException("Unsupported transaction type for event " + eventId + ": " + type);
                }
                LOGGER.info("Reversed balance change for account {}", event.accountId());
            }

            ensureAccountUpdated(accountRepository.decrementTotalTransactions(event.accountId()), event.accountId(), eventId);
            LOGGER.info("Reversed transaction stats for account {}", event.accountId());

            markEventAsProcessed(eventId);
        } finally {
            MDC.clear();
        }
    }

    private static String eventId(Long transactionId, String suffix) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction event is missing transactionId");
        }
        return transactionId + "-" + suffix;
    }

    private static String requireType(String type, String eventId) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Transaction event " + eventId + " is missing type");
        }
        return type;
    }

    private static Double requireAmount(Double amount, String eventId) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("Transaction event " + eventId + " has invalid amount");
        }
        return amount;
    }

    private void updateBalance(Long accountId, double amount, String eventId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Transaction event " + eventId + " is missing accountId");
        }
        ensureAccountUpdated(accountRepository.updateBalance(accountId, amount), accountId, eventId);
    }

    private static void ensureAccountUpdated(int updatedRows, Long accountId, String eventId) {
        if (updatedRows == 0) {
            throw new IllegalStateException("Account " + accountId + " was not updated for event " + eventId);
        }
    }

    private static void putMdc(String routingKey, String correlationId, Long accountId) {
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        MDC.put("routingKey", routingKey);
        MDC.put("accountId", String.valueOf(accountId));
    }
}
