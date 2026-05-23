package com.team31.financetracker.reporting.saga;

import com.team31.financetracker.contracts.events.TransactionCompletedEvent;
import com.team31.financetracker.contracts.events.TransactionVoidedEvent;
import com.team31.financetracker.contracts.feign.UserServiceClient;
import com.team31.financetracker.reporting.messaging.consumers.TransactionEventConsumer;
import com.team31.financetracker.reporting.messaging.publishers.ReportEventPublisher;
import com.team31.financetracker.reporting.model.ReportStatus;
import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.repository.SavedReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class SagaE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
        // MongoDB: point to a non-existent host with 500ms timeout
        // Observer is soft-fail so this is fine per spec
        registry.add("spring.data.mongodb.uri", 
            () -> "mongodb://localhost:27017/test?serverSelectionTimeoutMS=500&connectTimeoutMS=500");
        registry.add("spring.jpa.properties.hibernate.enable_lazy_load_no_trans", () -> "true");
    }

    @Autowired
    private TransactionEventConsumer consumer;

    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private SavedReportRepository reportRepository;

    @MockitoSpyBean
    private ReportEventPublisher reportEventPublisher;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private com.team31.financetracker.reporting.observer.MongoEventLogger mongoEventLogger;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void scenarioA_HappyPath() {
        // Scenario A: Happy path — transaction.completed → PENDING → GENERATED →
        // report.completed published
        Long transactionId = 1001L;
        Long userId = 10L;
        Long accountId = 100L;

        // Mock Feign success
        when(userServiceClient.getUser(userId)).thenReturn(null);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                transactionId, userId, accountId, "INCOME", "DEPOSIT", 1000.0, LocalDateTime.now());

        consumer.onTransactionCompleted(event);

        // Transaction has now committed — query directly via native SQL to bypass JPA cache
        String status = (String) entityManager.createNativeQuery(
            "SELECT status FROM saved_reports WHERE report_config->>'transactionId' = :txnId")
            .setParameter("txnId", transactionId.toString())
            .getSingleResult();

        assertThat(status).isEqualTo("GENERATED");

        // Also verify publisher calls
        Optional<SavedReport> reportOpt = reportRepository.findSagaReportByTransactionId(transactionId);
        assertThat(reportOpt).isPresent();
        SavedReport report = reportOpt.get();
        verify(reportEventPublisher).publishReportInitiated(report.getId(), transactionId, userId);
        verify(reportEventPublisher).publishReportCompleted(eq(report.getId()), eq(transactionId), eq(userId), any(), any());
    }

    @Test
    void scenarioB_FailureAndCompensation() {
        Long transactionId = 1002L;
        Long userId = 11L;
        Long accountId = 101L;

        when(userServiceClient.getUser(userId)).thenReturn(null);

        // Step 1: Run happy path first to create a SavedReport
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                transactionId, userId, accountId, "EXPENSE", "WITHDRAWAL", 500.0, LocalDateTime.now());
        consumer.onTransactionCompleted(event);

        Optional<SavedReport> reportOpt = reportRepository.findSagaReportByTransactionId(transactionId);
        assertThat(reportOpt).isPresent();
        SavedReport report = reportOpt.get();

        // Step 2: Manually force to FAILED (simulates simulateSnapshotFailure flag)
        reportRepository.transitionStatus(report.getId(), ReportStatus.GENERATED, ReportStatus.FAILED);

        // Step 3: Trigger compensation via transaction.voided
        TransactionVoidedEvent voidEvent = new TransactionVoidedEvent(
                transactionId, userId, accountId, null,
                "EXPENSE", "WITHDRAWAL", 500.0, "REPORT_FAILED", "simulateSnapshotFailure=true");
        consumer.onTransactionVoided(voidEvent);

        // Assert SavedReport is ARCHIVED
        SavedReport archived = reportRepository.findSagaReportByTransactionId(transactionId).orElseThrow();
        assertThat(archived.getStatus()).isEqualTo(ReportStatus.ARCHIVED);
        assertThat(archived.getReportConfig()).containsEntry("archiveReason", "transaction_voided");

        // Assert report.reverted was published
        verify(reportEventPublisher).publishReportReverted(archived.getId(), transactionId, userId);
    }

    @Test
    void scenarioC_PreCheckFailure() {
        // Scenario C: Pre-check failure — unknown userId → Feign 404 → no SavedReport
        // created, no events published
        Long transactionId = 1003L;
        Long userId = 99L;
        Long accountId = 102L;

        // Mock Feign 404
        feign.FeignException.NotFound notFound = new feign.FeignException.NotFound(
                "User not found",
                feign.Request.create(feign.Request.HttpMethod.GET, "/api/users/999",
                        java.util.Collections.emptyMap(), null, null, null),
                null,
                java.util.Collections.emptyMap());
        when(userServiceClient.getUser(userId)).thenThrow(notFound);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                transactionId, userId, accountId, "EXPENSE", "WITHDRAWAL", 200.0, LocalDateTime.now());

        consumer.onTransactionCompleted(event);

        // Verify DB State
        Optional<SavedReport> reportOpt = reportRepository.findSagaReportByTransactionId(transactionId);
        assertThat(reportOpt).isEmpty();

        // Verify no publishing
        verify(reportEventPublisher, never()).publishReportInitiated(anyLong(), anyLong(), anyLong());
    }

    @Test
    void extra_Idempotency() {
        // Extra: Idempotency — duplicate event delivery produces only one SavedReport
        Long transactionId = 1004L;
        Long userId = 12L;
        Long accountId = 103L;

        when(userServiceClient.getUser(userId)).thenReturn(null);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                transactionId, userId, accountId, "INCOME", "DEPOSIT", 1000.0, LocalDateTime.now());

        // Deliver multiple times
        consumer.onTransactionCompleted(event);
        consumer.onTransactionCompleted(event);
        consumer.onTransactionCompleted(event);

        // Verify only 1 record created
        long count = reportRepository.count();
        assertThat(count).isEqualTo(1L);

        Optional<SavedReport> reportOpt = reportRepository.findSagaReportByTransactionId(transactionId);
        assertThat(reportOpt).isPresent();

        // Verify publisher initiated called exactly once
        verify(reportEventPublisher, times(1)).publishReportInitiated(reportOpt.get().getId(), transactionId, userId);
    }
}
