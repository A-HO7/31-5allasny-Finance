package com.team31.financetracker.reporting.messaging;

import com.team31.financetracker.contracts.events.AccountRatedEvent;
import com.team31.financetracker.contracts.events.AccountStatsUpdatedEvent;
import com.team31.financetracker.contracts.events.TransactionCompletedEvent;
import com.team31.financetracker.contracts.events.TransactionVoidedEvent;
import com.team31.financetracker.contracts.feign.UserServiceClient;
import com.team31.financetracker.reporting.messaging.consumers.AccountEventConsumer;
import com.team31.financetracker.reporting.messaging.consumers.TransactionEventConsumer;
import com.team31.financetracker.reporting.messaging.publishers.ReportEventPublisher;
import com.team31.financetracker.reporting.model.ReportStatus;
import com.team31.financetracker.reporting.model.ReportType;
import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.observer.EntityObserver;
import com.team31.financetracker.contracts.dto.UserDTO;
import com.team31.financetracker.reporting.repository.SavedReportRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * S5-EVENTS Member B — Unit Test Suite
 *
 * Covers all logic required by the M3Plan for Member B:
 *   - TransactionEventConsumer.onTransactionCompleted  (Branch 3)
 *   - TransactionEventConsumer.onTransactionVoided     (Branch 4)
 *   - AccountEventConsumer.onAccountRated              (Branch 5)
 *   - AccountEventConsumer.onAccountStatsUpdated       (Branch 5)
 *
 * Publishers (Branch 2) are verified via Mockito interaction assertions
 * on the injected ReportEventPublisher mock.
 *
 * No Docker / Testcontainers required — pure unit tests using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class MemberBSagaUnitTest {

    // ─── Shared mocks ─────────────────────────────────────────────────────────
    @Mock SavedReportRepository  reportRepository;
    @Mock ReportEventPublisher   publisher;
    @Mock UserServiceClient      userServiceClient;
    @Mock EntityObserver         observer;

    TransactionEventConsumer transactionConsumer;
    AccountEventConsumer     accountConsumer;

    @BeforeEach
    void setUp() {
        transactionConsumer = new TransactionEventConsumer(
                reportRepository, publisher, userServiceClient, List.of(observer));
        accountConsumer = new AccountEventConsumer(List.of(observer));
    }

    // =========================================================================
    // Branch 3 — transaction.completed → forward saga path
    // =========================================================================

    @Nested
    @DisplayName("Branch 3: transaction.completed — forward saga")
    class TransactionCompleted {

        private TransactionCompletedEvent normalEvent() {
            return new TransactionCompletedEvent(
                    1L, 10L, 100L, "FOOD", "EXPENSE", 500.0,
                    LocalDateTime.of(2025, 1, 15, 10, 0));
        }

        @Test
        @DisplayName("Happy path: inserts PENDING, publishes report.initiated, transitions to GENERATED, publishes report.completed")
        void happyPath_fullSagaForwardFlow() {
            TransactionCompletedEvent event = normalEvent();

            // Idempotency check: no existing report
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.empty());

            // Feign: user exists — returns a valid UserDTO
            when(userServiceClient.getUser(10L)).thenReturn(UserDTO.builder().id(10L).email("test@example.com").status("ACTIVE").build());

            // Save returns report with id=99
            SavedReport saved = new SavedReport();
            saved.setId(99L);
            saved.setStatus(ReportStatus.PENDING);
            saved.setReportType(ReportType.CUSTOM);
            saved.setPeriodStart(LocalDate.of(2025, 1, 15));
            saved.setPeriodEnd(LocalDate.of(2025, 1, 15));
            saved.setReportConfig(new LinkedHashMap<>(Map.of(
                    "snapshotKind", "audit", "transactionId", 1L)));
            when(reportRepository.save(any())).thenReturn(saved);

            // transitionStatus: PENDING → GENERATED succeeds (1 row)
            when(reportRepository.transitionStatus(99L, ReportStatus.PENDING, ReportStatus.GENERATED))
                    .thenReturn(1);

            transactionConsumer.onTransactionCompleted(event);

            // ── Assert DB operations ──
            ArgumentCaptor<SavedReport> captor = ArgumentCaptor.forClass(SavedReport.class);
            verify(reportRepository).save(captor.capture());
            SavedReport toSave = captor.getValue();
            assertThat(toSave.getStatus()).isEqualTo(ReportStatus.PENDING);
            assertThat(toSave.getReportType()).isEqualTo(ReportType.CUSTOM);
            assertThat(toSave.getUserId()).isEqualTo(10L);
            assertThat(toSave.getReportConfig()).containsEntry("snapshotKind", "audit");
            assertThat(toSave.getReportConfig()).containsEntry("transactionId", 1L);

            // ── Assert publishers called in order ──
            verify(publisher).publishReportInitiated(99L, 1L, 10L);
            verify(publisher).publishReportCompleted(99L, 1L, 10L, "CUSTOM", 0.0);
            verify(publisher, never()).publishReportFailed(anyLong(), anyLong(), anyLong(), any());

            // ── Assert observer notified with GENERATED ──
            ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
            verify(observer).onEvent(actionCaptor.capture(), any());
            assertThat(actionCaptor.getValue()).isEqualTo("GENERATED");
        }

        @Test
        @DisplayName("Idempotency: if report already exists for transactionId, consumer is a no-op")
        void idempotency_duplicateEvent_noOp() {
            SavedReport existing = new SavedReport();
            existing.setId(5L);
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.of(existing));

            transactionConsumer.onTransactionCompleted(normalEvent());

            verify(reportRepository, never()).save(any());
            verify(publisher, never()).publishReportInitiated(anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("Feign 404: aborts saga — no DB insert, no publish")
        void feignNotFound_abortsConsumer() {
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.empty());
            doThrow(FeignException.NotFound.class).when(userServiceClient).getUser(10L);

            transactionConsumer.onTransactionCompleted(normalEvent());

            verify(reportRepository, never()).save(any());
            verify(publisher, never()).publishReportInitiated(anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("Feign error (non-404): aborts saga gracefully — no DB insert, no publish")
        void feignServiceUnavailable_abortsConsumer() {
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.empty());
            doThrow(FeignException.ServiceUnavailable.class).when(userServiceClient).getUser(10L);

            transactionConsumer.onTransactionCompleted(normalEvent());

            verify(reportRepository, never()).save(any());
            verify(publisher, never()).publishReportInitiated(anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("Simulated failure: state transitions to FAILED, publishes report.failed (not report.completed)")
        void simulatedFailure_publishesReportFailed() {
            // Event with simulateSnapshotFailure flag embedded in config
            TransactionCompletedEvent event = normalEvent();
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.empty());
            when(userServiceClient.getUser(10L)).thenReturn(UserDTO.builder().id(10L).email("test@example.com").status("ACTIVE").build());

            // The save returns a report whose reportConfig contains simulateSnapshotFailure=true
            SavedReport saved = new SavedReport();
            saved.setId(77L);
            saved.setStatus(ReportStatus.PENDING);
            saved.setReportType(ReportType.CUSTOM);
            saved.setPeriodStart(LocalDate.now());
            saved.setPeriodEnd(LocalDate.now());
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("snapshotKind", "audit");
            config.put("transactionId", 1L);
            config.put("simulateSnapshotFailure", true);   // ← failure flag
            saved.setReportConfig(config);
            when(reportRepository.save(any())).thenReturn(saved);

            // transitionStatus: PENDING → FAILED succeeds (lenient: strict mode safe-guard)
            lenient().when(reportRepository.transitionStatus(77L, ReportStatus.PENDING, ReportStatus.FAILED))
                    .thenReturn(1);

            transactionConsumer.onTransactionCompleted(event);

            verify(publisher).publishReportInitiated(77L, 1L, 10L);
            verify(publisher).publishReportFailed(eq(77L), eq(1L), eq(10L), any());
            verify(publisher, never()).publishReportCompleted(anyLong(), anyLong(), anyLong(), any(), any());

            ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
            verify(observer).onEvent(actionCaptor.capture(), any());
            assertThat(actionCaptor.getValue()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("transitionStatus returns 0 (concurrent thread won): skips publish to avoid double-fire")
        void concurrentTransition_zeroRows_skipsPublish() {
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.empty());
            when(userServiceClient.getUser(10L)).thenReturn(UserDTO.builder().id(10L).email("test@example.com").status("ACTIVE").build());

            SavedReport saved = new SavedReport();
            saved.setId(55L);
            saved.setStatus(ReportStatus.PENDING);
            saved.setReportType(ReportType.CUSTOM);
            saved.setPeriodStart(LocalDate.now());
            saved.setPeriodEnd(LocalDate.now());
            saved.setReportConfig(new LinkedHashMap<>(Map.of("snapshotKind", "audit", "transactionId", 1L)));
            when(reportRepository.save(any())).thenReturn(saved);

            // Another thread already transitioned it
            when(reportRepository.transitionStatus(55L, ReportStatus.PENDING, ReportStatus.GENERATED))
                    .thenReturn(0);

            transactionConsumer.onTransactionCompleted(normalEvent());

            // report.initiated is published (before transition)
            verify(publisher).publishReportInitiated(55L, 1L, 10L);
            // but report.completed is NOT published
            verify(publisher, never()).publishReportCompleted(anyLong(), anyLong(), anyLong(), any(), any());
        }

        @Test
        @DisplayName("MongoDB observer throws: saga continues — report.completed still published")
        void mongoObserverFails_sagaStillCompletes() {
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.empty());
            when(userServiceClient.getUser(10L)).thenReturn(UserDTO.builder().id(10L).email("test@example.com").status("ACTIVE").build());

            SavedReport saved = new SavedReport();
            saved.setId(33L);
            saved.setStatus(ReportStatus.PENDING);
            saved.setReportType(ReportType.CUSTOM);
            saved.setPeriodStart(LocalDate.now());
            saved.setPeriodEnd(LocalDate.now());
            saved.setReportConfig(new LinkedHashMap<>(Map.of("snapshotKind", "audit", "transactionId", 1L)));
            when(reportRepository.save(any())).thenReturn(saved);
            when(reportRepository.transitionStatus(33L, ReportStatus.PENDING, ReportStatus.GENERATED))
                    .thenReturn(1);

            // MongoDB is down
            doThrow(new RuntimeException("MongoDB connection refused"))
                    .when(observer).onEvent(any(), any());

            // Must NOT throw — soft fail
            transactionConsumer.onTransactionCompleted(normalEvent());

            // report.completed must still be published despite observer failure
            verify(publisher).publishReportCompleted(33L, 1L, 10L, "CUSTOM", 0.0);
        }
    }

    // =========================================================================
    // Branch 4 — transaction.voided → compensation path
    // =========================================================================

    @Nested
    @DisplayName("Branch 4: transaction.voided — compensation saga")
    class TransactionVoided {

        private TransactionVoidedEvent voidEvent() {
            return new TransactionVoidedEvent(
                    1L, 10L, 100L, null, "FOOD", "EXPENSE",
                    500.0, "COMPLETED", "account_frozen");
        }

        private SavedReport generatedReport() {
            SavedReport r = new SavedReport();
            r.setId(99L);
            r.setStatus(ReportStatus.GENERATED);
            r.setUserId(10L);
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("snapshotKind", "audit");
            config.put("transactionId", 1L);
            r.setReportConfig(config);
            return r;
        }

        @Test
        @DisplayName("Happy path: archives report, appends JSONB metadata, publishes report.reverted")
        void happyPath_archivesAndPublishes() {
            SavedReport report = generatedReport();
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.of(report));
            when(reportRepository.save(any())).thenReturn(report);

            transactionConsumer.onTransactionVoided(voidEvent());

            // ── Verify status set to ARCHIVED ──
            ArgumentCaptor<SavedReport> captor = ArgumentCaptor.forClass(SavedReport.class);
            verify(reportRepository).save(captor.capture());
            SavedReport archived = captor.getValue();
            assertThat(archived.getStatus()).isEqualTo(ReportStatus.ARCHIVED);

            // ── Verify JSONB enrichment ──
            assertThat(archived.getReportConfig()).containsKey("archiveReason");
            assertThat(archived.getReportConfig().get("archiveReason")).isEqualTo("transaction_voided");
            assertThat(archived.getReportConfig()).containsKey("voidReason");
            assertThat(archived.getReportConfig().get("voidReason")).isEqualTo("account_frozen");

            // ── Verify saga continuation ──
            verify(publisher).publishReportReverted(99L, 1L, 10L);

            // ── Verify audit observer called with ARCHIVED ──
            ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
            verify(observer).onEvent(actionCaptor.capture(), any());
            assertThat(actionCaptor.getValue()).isEqualTo("ARCHIVED");
        }

        @Test
        @DisplayName("No-op: no saga SavedReport found (void before snapshot)")
        void noReport_noOp() {
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.empty());

            transactionConsumer.onTransactionVoided(voidEvent());

            verify(reportRepository, never()).save(any());
            verify(publisher, never()).publishReportReverted(anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("Idempotency: report already ARCHIVED — skip all mutations")
        void alreadyArchived_idempotentNoOp() {
            SavedReport report = generatedReport();
            report.setStatus(ReportStatus.ARCHIVED);
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.of(report));

            transactionConsumer.onTransactionVoided(voidEvent());

            verify(reportRepository, never()).save(any());
            verify(publisher, never()).publishReportReverted(anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("MongoDB observer throws: report.reverted still published (soft-fail)")
        void mongoObserverFails_revertedStillPublished() {
            SavedReport report = generatedReport();
            when(reportRepository.findSagaReportByTransactionId(1L)).thenReturn(Optional.of(report));
            when(reportRepository.save(any())).thenReturn(report);
            doThrow(new RuntimeException("Mongo is down")).when(observer).onEvent(any(), any());

            transactionConsumer.onTransactionVoided(voidEvent());

            // Must still publish compensation event
            verify(publisher).publishReportReverted(99L, 1L, 10L);
        }
    }

    // =========================================================================
    // Branch 5 — account.rated + account.stats-updated observability consumers
    // =========================================================================

    @Nested
    @DisplayName("Branch 5: Observability consumers — account.rated and account.stats-updated")
    class ObservabilityConsumers {

        @Test
        @DisplayName("account.rated: fires observer with action=RATED and correct details")
        void accountRated_firesObserverWithRatedAction() {
            AccountRatedEvent event = new AccountRatedEvent(200L, 300L, 4.7);

            accountConsumer.onAccountRated(event);

            ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(observer).onEvent(actionCaptor.capture(), payloadCaptor.capture());

            assertThat(actionCaptor.getValue()).isEqualTo("RATED");

            Map<String, Object> payload = payloadCaptor.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) payload.get("details");
            assertThat(details).containsEntry("accountId", 200L);
            assertThat(details).containsEntry("statementId", 300L);
            assertThat(details).containsEntry("rating", 4.7);

            // Per spec: reportType and pagesGenerated must be null for account events
            assertThat(payload.get("reportType")).isNull();
            assertThat(payload.get("pagesGenerated")).isNull();
        }

        @Test
        @DisplayName("account.stats-updated: fires observer with action=STATS_UPDATED and correct details")
        void accountStatsUpdated_firesObserverWithStatsUpdatedAction() {
            AccountStatsUpdatedEvent event = new AccountStatsUpdatedEvent(200L, 3, 250.0);

            accountConsumer.onAccountStatsUpdated(event);

            ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(observer).onEvent(actionCaptor.capture(), payloadCaptor.capture());

            assertThat(actionCaptor.getValue()).isEqualTo("STATS_UPDATED");

            Map<String, Object> payload = payloadCaptor.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) payload.get("details");
            assertThat(details).containsEntry("accountId", 200L);
            assertThat(details).containsEntry("transactionDelta", 3);
            assertThat(details).containsEntry("balanceDelta", 250.0);

            // Per spec: reportType and pagesGenerated must be null
            assertThat(payload.get("reportType")).isNull();
            assertThat(payload.get("pagesGenerated")).isNull();
        }

        @Test
        @DisplayName("account.rated MongoDB failure: swallowed silently (soft-fail, no rethrow)")
        void accountRated_mongoFails_doesNotThrow() {
            AccountRatedEvent event = new AccountRatedEvent(200L, 300L, 4.7);
            doThrow(new RuntimeException("MongoDB timeout")).when(observer).onEvent(any(), any());

            // Must not throw — soft-fail policy
            accountConsumer.onAccountRated(event);
        }

        @Test
        @DisplayName("account.stats-updated MongoDB failure: swallowed silently (soft-fail)")
        void accountStatsUpdated_mongoFails_doesNotThrow() {
            AccountStatsUpdatedEvent event = new AccountStatsUpdatedEvent(200L, 1, 100.0);
            doThrow(new RuntimeException("MongoDB timeout")).when(observer).onEvent(any(), any());

            // Must not throw
            accountConsumer.onAccountStatsUpdated(event);
        }
    }
}
