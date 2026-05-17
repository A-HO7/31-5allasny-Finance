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

    @RabbitListener(queues = "${rabbitmq.queues.transaction-saga-listener}")
    @Transactional
    public void onTransactionApproved(TransactionApprovedEvent event, @Header("amqp_receivedRoutingKey") String routingKey, @Header(value = "x-correlation-id", required = false) String correlationId) {
        String eventId = event.transactionId() + "-APPROVED";
        if (isEventProcessed(eventId)) {
            LOGGER.warn("Event {} already processed.", eventId);
            return;
        }

        if (correlationId != null) MDC.put("correlationId", correlationId);
        MDC.put("routingKey", routingKey);
        MDC.put("accountId", String.valueOf(event.accountId()));

        LOGGER.info("Processing transaction.approved event for account {}", event.accountId());

        String type = event.type();
        Double amount = event.amount();

        if ("EXPENSE".equalsIgnoreCase(type)) {
            accountRepository.updateBalance(event.accountId(), -amount);
        } else if ("INCOME".equalsIgnoreCase(type)) {
            accountRepository.updateBalance(event.accountId(), amount);
        } else if ("TRANSFER".equalsIgnoreCase(type)) {
            accountRepository.updateBalance(event.accountId(), -amount);
            if (event.toAccountId() != null) {
                accountRepository.updateBalance(event.toAccountId(), amount);
            }
        }

        LOGGER.info("Updated balance for account {}", event.accountId());
        markEventAsProcessed(eventId);
        MDC.clear();
    }

    @RabbitListener(queues = "${rabbitmq.queues.transaction-saga-listener}")
    @Transactional
    public void onTransactionCompleted(TransactionCompletedEvent event, @Header("amqp_receivedRoutingKey") String routingKey, @Header(value = "x-correlation-id", required = false) String correlationId) {
        String eventId = event.transactionId() + "-COMPLETED";
        if (isEventProcessed(eventId)) {
            LOGGER.warn("Event {} already processed.", eventId);
            return;
        }

        if (correlationId != null) MDC.put("correlationId", correlationId);
        MDC.put("routingKey", routingKey);
        MDC.put("accountId", String.valueOf(event.accountId()));

        LOGGER.info("Processing transaction.completed event for account {}", event.accountId());
        accountRepository.incrementTotalTransactions(event.accountId());
        accountRepository.updateLastTransactionDate(event.accountId(), event.completedAt());
        LOGGER.info("Updated transaction stats for account {}", event.accountId());

        Account account = accountRepository.findById(event.accountId()).orElseThrow();
        accountEventPublisher.publishAccountStatsUpdatedEvent(
                new AccountStatsUpdatedEvent(account.getId(), 1, 0.0)
        );

        markEventAsProcessed(eventId);
        MDC.clear();
    }

    @RabbitListener(queues = "${rabbitmq.queues.transaction-saga-listener}")
    @Transactional
    public void onTransactionVoided(TransactionVoidedEvent event, @Header("amqp_receivedRoutingKey") String routingKey, @Header(value = "x-correlation-id", required = false) String correlationId) {
        String eventId = event.transactionId() + "-VOIDED";
        if (isEventProcessed(eventId)) {
            LOGGER.warn("Event {} already processed.", eventId);
            return;
        }

        if (correlationId != null) MDC.put("correlationId", correlationId);
        MDC.put("routingKey", routingKey);
        MDC.put("accountId", String.valueOf(event.accountId()));

        LOGGER.info("Processing transaction.voided event for account {}", event.accountId());

        String previousStatus = event.previousStatus();
        if ("APPROVED".equalsIgnoreCase(previousStatus) || "COMPLETING".equalsIgnoreCase(previousStatus) || "REPORT_PENDING".equalsIgnoreCase(previousStatus) || "REPORT_FAILED".equalsIgnoreCase(previousStatus)) {
            String type = event.type();
            Double amount = event.amount();

            if ("EXPENSE".equalsIgnoreCase(type)) {
                accountRepository.updateBalance(event.accountId(), amount);
            } else if ("INCOME".equalsIgnoreCase(type)) {
                accountRepository.updateBalance(event.accountId(), -amount);
            } else if ("TRANSFER".equalsIgnoreCase(type)) {
                accountRepository.updateBalance(event.accountId(), amount);
                if (event.toAccountId() != null) {
                    accountRepository.updateBalance(event.toAccountId(), -amount);
                }
            }
            LOGGER.info("Reversed balance change for account {}", event.accountId());
        }

        accountRepository.decrementTotalTransactions(event.accountId());
        LOGGER.info("Reversed transaction stats for account {}", event.accountId());

        markEventAsProcessed(eventId);
        MDC.clear();
    }
}
