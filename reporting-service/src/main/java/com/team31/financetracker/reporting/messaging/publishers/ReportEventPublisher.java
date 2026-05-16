package com.team31.financetracker.reporting.messaging.publishers;

import com.team31.financetracker.contracts.events.ReportCompletedEvent;
import com.team31.financetracker.contracts.events.ReportFailedEvent;
import com.team31.financetracker.contracts.events.ReportInitiatedEvent;
import com.team31.financetracker.contracts.events.ReportRevertedEvent;
import com.team31.financetracker.reporting.config.ReportEventConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * S5-EVENTS — Task 2: Publishers.
 *
 * Publishes all four report.* events to the report.events TopicExchange.
 * Every publish is logged at INFO level so Grafana Loki can track saga progress.
 *
 * Routing key → consumer:
 *   report.initiated  → transaction-service (sets Transaction.status = REPORT_PENDING)
 *   report.completed  → transaction-service (sets Transaction.status = REPORTED)
 *   report.failed     → transaction-service (sets Transaction.status = REPORT_FAILED → triggers void)
 *   report.reverted   → transaction-service (sets Transaction.status = REVERTED)
 */
@Component
public class ReportEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReportEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ReportEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // ─── report.initiated ────────────────────────────────────────────────────

    /**
     * Published immediately after the PENDING SavedReport row is inserted.
     * Signals to S3 that the saga snapshot has started.
     */
    public void publishReportInitiated(Long reportId, Long transactionId, Long userId) {
        ReportInitiatedEvent event = new ReportInitiatedEvent(reportId, transactionId, userId);
        rabbitTemplate.convertAndSend(
                ReportEventConfig.REPORT_EXCHANGE,
                ReportEventConfig.RK_REPORT_INITIATED,
                event
        );
        log.info("Published {} for reportId={} transactionId={} userId={}",
                ReportEventConfig.RK_REPORT_INITIATED, reportId, transactionId, userId);
    }

    // ─── report.completed ────────────────────────────────────────────────────

    /**
     * Published after the SavedReport transitions to GENERATED.
     * Signals to S3 to set Transaction.status = REPORTED (saga happy path complete).
     */
    public void publishReportCompleted(Long reportId, Long transactionId, Long userId,
                                       String reportType, Double pagesGenerated) {
        ReportCompletedEvent event = new ReportCompletedEvent(
                reportId, transactionId, userId, reportType, pagesGenerated);
        rabbitTemplate.convertAndSend(
                ReportEventConfig.REPORT_EXCHANGE,
                ReportEventConfig.RK_REPORT_COMPLETED,
                event
        );
        log.info("Published {} for reportId={} transactionId={} userId={}",
                ReportEventConfig.RK_REPORT_COMPLETED, reportId, transactionId, userId);
    }

    // ─── report.failed ───────────────────────────────────────────────────────

    /**
     * Published when snapshot generation fails (e.g. simulateSnapshotFailure=true).
     * Triggers compensation: S3 voids the transaction → account/budget roll back.
     */
    public void publishReportFailed(Long reportId, Long transactionId, Long userId, String reason) {
        ReportFailedEvent event = new ReportFailedEvent(reportId, transactionId, userId, reason);
        rabbitTemplate.convertAndSend(
                ReportEventConfig.REPORT_EXCHANGE,
                ReportEventConfig.RK_REPORT_FAILED,
                event
        );
        log.info("Published {} for reportId={} transactionId={} userId={} reason={}",
                ReportEventConfig.RK_REPORT_FAILED, reportId, transactionId, userId, reason);
    }

    // ─── report.reverted ─────────────────────────────────────────────────────

    /**
     * Published after the saga SavedReport is archived in response to transaction.voided.
     * Signals to S3 that the compensation cascade is complete on S5's side.
     */
    public void publishReportReverted(Long reportId, Long transactionId, Long userId) {
        ReportRevertedEvent event = new ReportRevertedEvent(reportId, transactionId, userId);
        rabbitTemplate.convertAndSend(
                ReportEventConfig.REPORT_EXCHANGE,
                ReportEventConfig.RK_REPORT_REVERTED,
                event
        );
        log.info("Published {} for reportId={} transactionId={} userId={}",
                ReportEventConfig.RK_REPORT_REVERTED, reportId, transactionId, userId);
    }
}
