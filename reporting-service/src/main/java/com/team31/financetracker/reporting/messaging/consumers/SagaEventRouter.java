package com.team31.financetracker.reporting.messaging.consumers;

import com.team31.financetracker.contracts.events.AccountRatedEvent;
import com.team31.financetracker.contracts.events.AccountStatsUpdatedEvent;
import com.team31.financetracker.contracts.events.TransactionCompletedEvent;
import com.team31.financetracker.contracts.events.TransactionVoidedEvent;
import com.team31.financetracker.reporting.config.ReportEventConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * S5-EVENTS — Single entry point for the report.saga-listener queue.
 *
 * Spring AMQP creates ONE listener container for this queue.
 * Dispatch to the correct handler is done by Spring AMQP based on
 * the deserialized payload type (@RabbitHandler).
 *
 * Pattern: class-level @RabbitListener + method-level @RabbitHandler
 * This is the correct Spring AMQP routing pattern (not `condition` which
 * belongs to Spring Kafka's @KafkaListener).
 *
 * Event types bound to report.saga-listener:
 *   transaction.completed  → TransactionCompletedEvent → TransactionEventConsumer
 *   transaction.voided     → TransactionVoidedEvent    → TransactionEventConsumer
 *   account.rated          → AccountRatedEvent         → AccountEventConsumer
 *   account.stats-updated  → AccountStatsUpdatedEvent  → AccountEventConsumer
 */
@Component
@RabbitListener(queues = ReportEventConfig.SAGA_QUEUE)
public class SagaEventRouter {

    private static final Logger log = LoggerFactory.getLogger(SagaEventRouter.class);

    private final TransactionEventConsumer transactionConsumer;
    private final AccountEventConsumer     accountConsumer;

    public SagaEventRouter(TransactionEventConsumer transactionConsumer,
                           AccountEventConsumer accountConsumer) {
        this.transactionConsumer = transactionConsumer;
        this.accountConsumer     = accountConsumer;
    }

    @RabbitHandler
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        transactionConsumer.onTransactionCompleted(event);
    }

    @RabbitHandler
    public void handleTransactionVoided(TransactionVoidedEvent event) {
        transactionConsumer.onTransactionVoided(event);
    }

    @RabbitHandler
    public void handleAccountRated(AccountRatedEvent event) {
        accountConsumer.onAccountRated(event);
    }

    @RabbitHandler
    public void handleAccountStatsUpdated(AccountStatsUpdatedEvent event) {
        accountConsumer.onAccountStatsUpdated(event);
    }

    /** Catches any unrecognised payload — logs and discards, never throws. */
    @RabbitHandler(isDefault = true)
    public void handleUnknown(Object message) {
        log.warn("SagaEventRouter received unhandled message type: {}",
                message == null ? "null" : message.getClass().getName());
    }
}
