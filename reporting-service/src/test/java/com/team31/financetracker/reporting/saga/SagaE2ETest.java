package com.team31.financetracker.reporting.saga;

import com.team31.financetracker.contracts.events.TransactionCompletedEvent;
import com.team31.financetracker.contracts.events.TransactionVoidedEvent;
import com.team31.financetracker.contracts.feign.UserServiceClient;
import com.team31.financetracker.reporting.messaging.consumers.TransactionEventConsumer;
import com.team31.financetracker.reporting.messaging.publishers.ReportEventPublisher;
import com.team31.financetracker.reporting.model.ReportStatus;
import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.repository.SavedReportRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
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

    @Container
    static MongoDBContainer mongoDB = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);

        registry.add("spring.data.mongodb.uri", mongoDB::getReplicaSetUrl);
    }

    @Autowired
    private TransactionEventConsumer consumer;

    @SpyBean
    private SavedReportRepository reportRepository;

    @SpyBean
    private ReportEventPublisher reportEventPublisher;

    @MockBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
    }

    @Test
    void scenarioA_HappyPath() {
        // Scenario A: Happy path — transaction.completed → PENDING → GENERATED → report.completed published
        Long transactionId = 1001L;
        Long userId = 10L;
        Long accountId = 100L;

        // Mock Feign success
        when(userServiceClient.getUser(userId)).thenReturn(null);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                transactionId, userId, accountId, "INCOME", "DEPOSIT", 1000.0, LocalDateTime.now()
        );

        consumer.onTransactionCompleted(event);

        // Verify DB State
        Optional<SavedReport> reportOpt = reportRepository.findSagaReportByTransactionId(transactionId);
        assertThat(reportOpt).isPresent();
        SavedReport report = reportOpt.get();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATED);

        // Verify publisher calls
        verify(reportEventPublisher).publishReportInitiated(report.getId(), transactionId, userId);
        verify(reportEventPublisher).publishReportCompleted(eq(report.getId()), eq(transactionId), eq(userId), any(), any());
    }

    @Test
    void scenarioB_FailureAndCompensation() {
        // Scenario B: Failure + compensation — simulateSnapshotFailure → FAILED → transaction.voided → ARCHIVED → report.reverted published
        Long transactionId = 1002L;
        Long userId = 11L;
        Long accountId = 101L;

        // Mock Feign success
        when(userServiceClient.getUser(userId)).thenReturn(null);

        // Intercept save to inject simulateSnapshotFailure flag
        doAnswer(invocation -> {
            SavedReport r = invocation.getArgument(0);
            if (r.getReportConfig() != null) {
                r.getReportConfig().put("simulateSnapshotFailure", true);
            }
            return invocation.callRealMethod();
        }).when(reportRepository).save(any(SavedReport.class));

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                transactionId, userId, accountId, "EXPENSE", "WITHDRAWAL", 500.0, LocalDateTime.now()
        );

        // Act 1: Complete transaction (simulating failure)
        consumer.onTransactionCompleted(event);

        Optional<SavedReport> reportOpt = reportRepository.findSagaReportByTransactionId(transactionId);
        assertThat(reportOpt).isPresent();
        SavedReport report = reportOpt.get();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);

        verify(reportEventPublisher).publishReportFailed(eq(report.getId()), eq(transactionId), eq(userId), eq("simulateSnapshotFailure=true"));

        // Act 2: Void transaction (compensation)
        TransactionVoidedEvent voidEvent = new TransactionVoidedEvent(
                transactionId, userId, accountId, null, "EXPENSE", "WITHDRAWAL", 500.0, "PENDING", "Saga failure"
        );
        consumer.onTransactionVoided(voidEvent);

        // Verify DB State
        SavedReport archivedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(archivedReport.getStatus()).isEqualTo(ReportStatus.ARCHIVED);

        // Verify publisher calls
        verify(reportEventPublisher).publishReportReverted(archivedReport.getId(), transactionId, userId);
    }

    @Test
    void scenarioC_PreCheckFailure() {
        // Scenario C: Pre-check failure — unknown userId → Feign 404 → no SavedReport created, no events published
        Long transactionId = 1003L;
        Long userId = 99L;
        Long accountId = 102L;

        // Mock Feign 404
        FeignException.NotFound notFound = Mockito.mock(FeignException.NotFound.class);
        when(userServiceClient.getUser(userId)).thenThrow(notFound);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                transactionId, userId, accountId, "EXPENSE", "WITHDRAWAL", 200.0, LocalDateTime.now()
        );

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
                transactionId, userId, accountId, "INCOME", "DEPOSIT", 1000.0, LocalDateTime.now()
        );

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
