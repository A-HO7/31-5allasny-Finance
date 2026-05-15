package com.team31.financetracker.reporting.messaging.consumers;

import com.team31.financetracker.contracts.events.TransactionCompletedEvent;
import com.team31.financetracker.contracts.events.TransactionVoidedEvent;
import com.team31.financetracker.contracts.feign.UserServiceClient;
import com.team31.financetracker.reporting.config.ReportEventConfig;
import com.team31.financetracker.reporting.messaging.publishers.ReportEventPublisher;
import com.team31.financetracker.reporting.model.ReportStatus;
import com.team31.financetracker.reporting.model.ReportType;
import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.observer.EntityObserver;
import com.team31.financetracker.reporting.repository.SavedReportRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * S5-EVENTS — Task 3 (transaction.completed) + Task 4 (transaction.voided).
 *
 * Listens on report.saga-listener queue.
 * Routes to the correct handler based on the RabbitMQ message's routing key,
 * which Spring AMQP injects via @Header.
 *
 * Idempotency guarantee:
 *   - transaction.completed: findSagaReportByTransactionId() returns present → no-op.
 *   - transaction.voided:    same lookup; status already ARCHIVED → no-op.
 *
 * MongoDB soft-fail policy:
 *   Any Observer write failure is caught and logged WARN.
 *   It NEVER rolls back the PostgreSQL transaction or prevents publishing.
 */
@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final SavedReportRepository reportRepository;
    private final ReportEventPublisher  publisher;
    private final UserServiceClient     userServiceClient;
    private final List<EntityObserver>  observers;

    public TransactionEventConsumer(SavedReportRepository reportRepository,
                                    ReportEventPublisher publisher,
                                    UserServiceClient userServiceClient,
                                    List<EntityObserver> observers) {
        this.reportRepository  = reportRepository;
        this.publisher         = publisher;
        this.userServiceClient = userServiceClient;
        this.observers         = observers;
    }

    // =========================================================================
    // Task 3 — transaction.completed → forward saga path
    // =========================================================================

    @RabbitListener(queues = ReportEventConfig.SAGA_QUEUE,
                    condition = "headers['amqp_receivedRoutingKey'] == 'transaction.completed'")
    @Transactional
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        MDC.put("routingKey", ReportEventConfig.RK_TXN_COMPLETED);
        MDC.put("transactionId", String.valueOf(event.transactionId()));
        MDC.put("userId", String.valueOf(event.userId()));

        log.info("Received {} transactionId={} userId={}",
                ReportEventConfig.RK_TXN_COMPLETED, event.transactionId(), event.userId());

        try {
            // ── Step 1: Idempotency check ─────────────────────────────────────
            Optional<SavedReport> existing =
                    reportRepository.findSagaReportByTransactionId(event.transactionId());
            if (existing.isPresent()) {
                log.info("Idempotency: saga SavedReport already exists for transactionId={} — skipping",
                        event.transactionId());
                return;
            }

            // ── Step 2: Feign → user-service (verify user exists) ─────────────
            try {
                userServiceClient.getUser(event.userId());
            } catch (FeignException.NotFound e) {
                log.warn("user-service returned 404 for userId={} — aborting saga snapshot", event.userId());
                return;
            } catch (FeignException e) {
                log.warn("user-service unavailable for userId={}: {} — aborting saga snapshot",
                        event.userId(), e.getMessage());
                return;
            }

            // ── Step 3: Insert PENDING SavedReport ────────────────────────────
            LocalDate snapshotDate = event.completedAt() != null
                    ? event.completedAt().toLocalDate()
                    : LocalDate.now();

            Map<String, Object> reportConfig = new LinkedHashMap<>();
            reportConfig.put("snapshotKind", "audit");
            reportConfig.put("transactionId", event.transactionId());
            reportConfig.put("accountId",     event.accountId());
            reportConfig.put("category",      event.category());
            reportConfig.put("type",          event.type());
            reportConfig.put("amount",        event.amount());

            SavedReport report = new SavedReport();
            report.setUserId(event.userId());
            report.setName("Saga Snapshot — txn #" + event.transactionId());
            report.setReportType(ReportType.CUSTOM);
            report.setPeriodStart(snapshotDate);
            report.setPeriodEnd(snapshotDate);
            report.setStatus(ReportStatus.PENDING);
            report.setReportConfig(reportConfig);

            SavedReport saved = reportRepository.save(report);
            MDC.put("reportId", String.valueOf(saved.getId()));
            log.info("Inserted PENDING SavedReport id={} for transactionId={}", saved.getId(), event.transactionId());

            // ── Step 4: Publish report.initiated ─────────────────────────────
            publisher.publishReportInitiated(saved.getId(), event.transactionId(), event.userId());

            // ── Step 5: Check simulateSnapshotFailure flag in reportConfig ────
            boolean simulateFailure = Boolean.TRUE.equals(reportConfig.get("simulateSnapshotFailure"));

            if (simulateFailure) {
                // Failure branch: PENDING → FAILED
                int rows = reportRepository.transitionStatus(
                        saved.getId(), ReportStatus.PENDING, ReportStatus.FAILED);
                if (rows > 0) {
                    notifyObserversSoft("FAILED", saved, "CUSTOM", 0.0);
                    publisher.publishReportFailed(
                            saved.getId(), event.transactionId(), event.userId(),
                            "simulateSnapshotFailure=true");
                    log.info("Snapshot FAILED (simulated) for reportId={}", saved.getId());
                }
                return;
            }

            // ── Step 6: State-conditional UPDATE PENDING → GENERATED ──────────
            int rows = reportRepository.transitionStatus(
                    saved.getId(), ReportStatus.PENDING, ReportStatus.GENERATED);

            if (rows == 0) {
                // Another thread already transitioned it — skip to avoid double-publish
                log.warn("transitionStatus returned 0 for reportId={} — concurrent processing, skipping", saved.getId());
                return;
            }

            // ── Step 7: Write GENERATED audit event to MongoDB (soft-fail) ────
            notifyObserversSoft("GENERATED", saved, "CUSTOM", 0.0);

            // ── Step 8: Publish report.completed ─────────────────────────────
            publisher.publishReportCompleted(
                    saved.getId(), event.transactionId(), event.userId(), "CUSTOM", 0.0);

            log.info("Saga snapshot GENERATED for reportId={} transactionId={}",
                    saved.getId(), event.transactionId());

        } finally {
            MDC.remove("routingKey");
            MDC.remove("transactionId");
            MDC.remove("userId");
            MDC.remove("reportId");
        }
    }

    // =========================================================================
    // Task 4 — transaction.voided → compensation path
    // =========================================================================

    @RabbitListener(queues = ReportEventConfig.SAGA_QUEUE,
                    condition = "headers['amqp_receivedRoutingKey'] == 'transaction.voided'")
    @Transactional
    public void onTransactionVoided(TransactionVoidedEvent event) {
        MDC.put("routingKey", ReportEventConfig.RK_TXN_VOIDED);
        MDC.put("transactionId", String.valueOf(event.transactionId()));

        log.info("Received {} transactionId={}", ReportEventConfig.RK_TXN_VOIDED, event.transactionId());

        try {
            // ── Step 1: Find saga SavedReport (idempotency guard) ─────────────
            Optional<SavedReport> opt =
                    reportRepository.findSagaReportByTransactionId(event.transactionId());

            if (opt.isEmpty()) {
                log.info("No saga SavedReport found for transactionId={} — void before snapshot, no-op",
                        event.transactionId());
                return;
            }

            SavedReport report = opt.get();

            // ── Step 2: Already archived → idempotent no-op ───────────────────
            if (ReportStatus.ARCHIVED == report.getStatus()) {
                log.info("Idempotency: SavedReport id={} already ARCHIVED — skipping", report.getId());
                return;
            }

            // ── Step 3: Archive the report ────────────────────────────────────
            report.setStatus(ReportStatus.ARCHIVED);
            Map<String, Object> config = report.getReportConfig();
            if (config == null) config = new LinkedHashMap<>();
            config.put("archiveReason", "transaction_voided");
            config.put("archivedAt", LocalDateTime.now().toString());
            config.put("voidReason", event.reason());
            report.setReportConfig(config);

            reportRepository.save(report);
            MDC.put("reportId", String.valueOf(report.getId()));
            log.info("Archived SavedReport id={} (compensation for transactionId={})",
                    report.getId(), event.transactionId());

            // ── Step 4: Write ARCHIVED audit event to MongoDB (soft-fail) ─────
            notifyObserversSoft("ARCHIVED", report, "CUSTOM", 0.0);

            // ── Step 5: Publish report.reverted ──────────────────────────────
            publisher.publishReportReverted(report.getId(), event.transactionId(), event.userId());

        } finally {
            MDC.remove("routingKey");
            MDC.remove("transactionId");
            MDC.remove("reportId");
        }
    }

    // =========================================================================
    // Helper: notify observer chain with MongoDB soft-fail guarantee
    // =========================================================================

    /**
     * Fires the Observer chain. Any MongoDB exception is caught + logged WARN.
     * Never rethrows — PostgreSQL transaction and RabbitMQ publish must not be
     * affected by a MongoDB failure.
     */
    private void notifyObserversSoft(String action, SavedReport report,
                                     String reportType, Double pagesGenerated) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("reportId",       report.getId());
            payload.put("reportType",     reportType);
            payload.put("pagesGenerated", pagesGenerated);
            payload.put("details",        report.getReportConfig());

            for (EntityObserver observer : observers) {
                observer.onEvent(action, payload);
            }
        } catch (Exception e) {
            log.warn("Observer chain failed for action='{}' reportId={}: {}",
                    action, report.getId(), e.getMessage());
        }
    }
}
