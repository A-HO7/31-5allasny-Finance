package com.team31.financetracker.reporting.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the Reporting Service (S5-EVENTS).
 *
 * What this class declares:
 *  - report.events        — TopicExchange that S5 PUBLISHES to
 *  - transaction.events   — TopicExchange reference (S5 CONSUMES from it via queue bindings)
 *  - account.events       — TopicExchange reference (S5 CONSUMES account.rated + account.stats-updated)
 *  - report.saga-listener — Consumer queue bound to BOTH exchanges above
 *  - report.saga-listener.dlq — Dead-letter queue; messages land here after 3 failed retries
 *
 * Routing keys consumed:
 *   transaction.completed   → forward saga path  (create PENDING snapshot)
 *   transaction.voided      → compensation path  (archive snapshot)
 *   account.rated           → observability only (audit enrichment)
 *   account.stats-updated   → observability only (audit enrichment)
 */
@Configuration
public class ReportEventConfig {

    // ─── Exchange names ───────────────────────────────────────────────────────

    public static final String REPORT_EXCHANGE        = "report.events";
    public static final String TRANSACTION_EXCHANGE   = "transaction.events";
    public static final String ACCOUNT_EXCHANGE       = "account.events";

    // ─── Queue / DLQ names ───────────────────────────────────────────────────

    public static final String SAGA_QUEUE             = "report.saga-listener";
    public static final String SAGA_DLQ               = "report.saga-listener.dlq";
    private static final String DLX_EXCHANGE          = "report.saga-listener.dlx";

    // ─── Routing keys this service CONSUMES ──────────────────────────────────

    public static final String RK_TXN_COMPLETED       = "transaction.completed";
    public static final String RK_TXN_VOIDED          = "transaction.voided";
    public static final String RK_ACCOUNT_RATED       = "account.rated";
    public static final String RK_ACCOUNT_STATS       = "account.stats-updated";

    // ─── Routing keys this service PUBLISHES ─────────────────────────────────

    public static final String RK_REPORT_INITIATED    = "report.initiated";
    public static final String RK_REPORT_COMPLETED    = "report.completed";
    public static final String RK_REPORT_FAILED       = "report.failed";
    public static final String RK_REPORT_REVERTED     = "report.reverted";

    // =========================================================================
    // Exchanges
    // =========================================================================

    /** Exchange that S5 publishes all report.* events to. */
    @Bean
    public TopicExchange reportEventsExchange() {
        return new TopicExchange(REPORT_EXCHANGE, true, false);
    }

    /** Reference to transaction.events exchange (producer-owned; S3 declared it). */
    @Bean
    public TopicExchange transactionEventsExchange() {
        return new TopicExchange(TRANSACTION_EXCHANGE, true, false);
    }

    /** Reference to account.events exchange (producer-owned; S2 declared it). */
    @Bean
    public TopicExchange accountEventsExchange() {
        return new TopicExchange(ACCOUNT_EXCHANGE, true, false);
    }

    /** Dead-letter exchange — receives rejected messages after max-retries (3). */
    @Bean
    public TopicExchange sagaDlx() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    // =========================================================================
    // Queues
    // =========================================================================

    /**
     * Main consumer queue.
     * x-dead-letter-exchange → DLX_EXCHANGE so that on retry exhaustion
     * (default-requeue-rejected: false + max-retries: 3) messages flow to the DLQ.
     */
    @Bean
    public Queue reportSagaListenerQueue() {
        return QueueBuilder.durable(SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SAGA_DLQ)
                .build();
    }

    /** Dead-letter queue — terminal sink for messages that failed 3 times. */
    @Bean
    public Queue reportSagaListenerDlq() {
        return QueueBuilder.durable(SAGA_DLQ).build();
    }

    // =========================================================================
    // Bindings — connect the consumer queue to the two upstream exchanges
    // =========================================================================

    @Bean
    public Binding bindTxnCompleted(Queue reportSagaListenerQueue,
                                    TopicExchange transactionEventsExchange) {
        return BindingBuilder.bind(reportSagaListenerQueue)
                .to(transactionEventsExchange)
                .with(RK_TXN_COMPLETED);
    }

    @Bean
    public Binding bindTxnVoided(Queue reportSagaListenerQueue,
                                 TopicExchange transactionEventsExchange) {
        return BindingBuilder.bind(reportSagaListenerQueue)
                .to(transactionEventsExchange)
                .with(RK_TXN_VOIDED);
    }

    @Bean
    public Binding bindAccountRated(Queue reportSagaListenerQueue,
                                    TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(reportSagaListenerQueue)
                .to(accountEventsExchange)
                .with(RK_ACCOUNT_RATED);
    }

    @Bean
    public Binding bindAccountStats(Queue reportSagaListenerQueue,
                                    TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(reportSagaListenerQueue)
                .to(accountEventsExchange)
                .with(RK_ACCOUNT_STATS);
    }

    /** Bind DLQ to DLX so messages are routable from the dead-letter exchange. */
    @Bean
    public Binding bindDlq(Queue reportSagaListenerDlq,
                            TopicExchange sagaDlx) {
        return BindingBuilder.bind(reportSagaListenerDlq)
                .to(sagaDlx)
                .with(SAGA_DLQ);
    }

    // =========================================================================
    // Message converter — JSON for all messages (uses Jackson)
    // =========================================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
