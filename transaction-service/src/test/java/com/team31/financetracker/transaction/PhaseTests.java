package com.team31.financetracker.transaction;

import com.team31.financetracker.transaction.mongodb.EventType;
import com.team31.financetracker.transaction.mongodb.MongoEvent;
import com.team31.financetracker.transaction.observer.EntityObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
public class PhaseTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private com.team31.financetracker.transaction.repository.TransactionRepository transactionRepository;

    @Autowired
    private com.team31.financetracker.transaction.repository.TransactionEventRepository transactionEventRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.team31.financetracker.transaction.security.JwtService jwtService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Nested
    @DisplayName("Phase 1: Structural Scenarios")
    class Phase1StructuralTests {

        @Test
        @DisplayName("Section 3.3 (a): Verify EntityObserver interface")
        void testEntityObserverInterface() throws Exception {
            Class<?> clazz = Class.forName("com.team31.financetracker.transaction.observer.EntityObserver");
            assertTrue(clazz.isInterface(), "EntityObserver must be an interface");
            Method onEvent = clazz.getMethod("onEvent", String.class, Object.class);
            assertThat(onEvent).isNotNull();
        }

        @Test
        @DisplayName("Section 3.3 (b): Verify MongoEventLogger implementation")
        void testMongoEventLoggerImplementation() throws Exception {
            Class<?> loggerClass = Class.forName("com.team31.financetracker.transaction.observer.MongoEventLogger");
            Class<?> observerInterface = Class.forName("com.team31.financetracker.transaction.observer.EntityObserver");
            assertThat(observerInterface.isAssignableFrom(loggerClass)).isTrue();
        }

        @Test
        @DisplayName("Section 3.7 (a): Verify MongoEvent interface")
        void testMongoEventInterface() throws Exception {
            Class<?> clazz = Class.forName("com.team31.financetracker.transaction.mongodb.MongoEvent");
            assertTrue(clazz.isInterface(), "MongoEvent must be an interface");
            assertThat(clazz.getMethod("getId")).isNotNull();
            assertThat(clazz.getMethod("getTimestamp")).isNotNull();
            assertThat(clazz.getMethod("getAction")).isNotNull();
            assertThat(clazz.getMethod("getDetails")).isNotNull();
        }

        @Test
        @DisplayName("Section 3.7 (b): Verify TransactionEvent implements MongoEvent")
        void testTransactionEventImplementation() throws Exception {
            Class<?> eventClass = Class.forName("com.team31.financetracker.transaction.mongodb.TransactionEvent");
            Class<?> mongoEventInterface = Class.forName("com.team31.financetracker.transaction.mongodb.MongoEvent");
            assertThat(mongoEventInterface.isAssignableFrom(eventClass)).isTrue();
        }

        @Test
        @DisplayName("Section 3.7 (c): Verify EventFactory skeleton")
        void testEventFactorySkeleton() throws Exception {
            Class<?> factoryClass = Class.forName("com.team31.financetracker.transaction.mongodb.EventFactory");
            Method createEvent = factoryClass.getMethod("createEvent", EventType.class, Map.class);
            assertThat(Modifier.isStatic(createEvent.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("Section 3.7 (d): Verify EventFactory returns TransactionEvent")
        void testEventFactoryDispatch() throws Exception {
            Map<String, Object> params = new HashMap<>();
            params.put("action", "TEST_ACTION");
            params.put("entityId", 123L);

            Class<?> factoryClass = Class.forName("com.team31.financetracker.transaction.mongodb.EventFactory");
            Method createEvent = factoryClass.getMethod("createEvent", EventType.class, Map.class);

            Object result = createEvent.invoke(null, EventType.TRANSACTION, params);
            assertThat(result).isNotNull();

            Class<?> transactionEventClass = Class.forName("com.team31.financetracker.transaction.mongodb.TransactionEvent");
            assertThat(transactionEventClass.isInstance(result)).isTrue();

            MongoEvent event = (MongoEvent) result;
            assertThat(event.getAction()).isEqualTo("TEST_ACTION");
        }

        @Test
        @DisplayName("Section 7.3: Verify Neo4j Entities")
        void testNeo4jEntities() throws Exception {
            Class.forName("com.team31.financetracker.transaction.neo4j.UserNode");
            Class.forName("com.team31.financetracker.transaction.neo4j.CategoryNode");
            Class.forName("com.team31.financetracker.transaction.neo4j.SpentOnRelationship");
        }
    }

    @Nested
    @DisplayName("Phase 2: Security & Wiring Scenarios")
    class Phase2SecurityTests {

        @Test
        @DisplayName("Section 3.4: Verify AuthHandler chain structure")
        void testAuthHandlerChain() throws Exception {
            Class<?> handlerClass = Class.forName("com.team31.financetracker.transaction.security.AuthHandler");
            assertThat(Modifier.isAbstract(handlerClass.getModifiers())).isTrue();
            assertThat(handlerClass.getMethod("setNext", handlerClass)).isNotNull();
            assertThat(handlerClass.getDeclaredMethod("handle",
                    Class.forName("com.team31.financetracker.transaction.security.AuthContext"),
                    jakarta.servlet.http.HttpServletResponse.class)).isNotNull();
        }

        @Test
        @DisplayName("Section 4.3 (c): Call M1 endpoint without token -> 401")
        void testM1EndpointNoToken() throws Exception {
            mockMvc.perform(get("/api/transactions")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Section 4.3 (c): Call dashboard analytics endpoint without token -> 401")
        void testDashboardAnalyticsNoToken() throws Exception {
            mockMvc.perform(get("/api/transactions/analytics/dashboard")
                    .param("startDate", "2026-03-01")
                    .param("endDate", "2026-03-31")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("M2: Dashboard analytics returns aggregates and logs event")
        void testDashboardAnalyticsReturnsCorrectTotalsAndLogsEvent() throws Exception {
            transactionRepository.deleteAll();
            transactionEventRepository.deleteAll();

            var baseDate = java.time.LocalDateTime.of(2026, 3, 15, 12, 0);
            createTransaction(1L, 1L, com.team31.financetracker.transaction.Enums.TransactionType.INCOME,
                    com.team31.financetracker.transaction.Enums.TransactionCategory.SALARY,
                    com.team31.financetracker.transaction.Enums.TransactionStatus.COMPLETED,
                    3000.0, baseDate.withDayOfMonth(1));
            createTransaction(1L, 1L, com.team31.financetracker.transaction.Enums.TransactionType.INCOME,
                    com.team31.financetracker.transaction.Enums.TransactionCategory.SALARY,
                    com.team31.financetracker.transaction.Enums.TransactionStatus.COMPLETED,
                    4000.0, baseDate.withDayOfMonth(5));
            createTransaction(1L, 1L, com.team31.financetracker.transaction.Enums.TransactionType.INCOME,
                    com.team31.financetracker.transaction.Enums.TransactionCategory.SALARY,
                    com.team31.financetracker.transaction.Enums.TransactionStatus.COMPLETED,
                    2000.0, baseDate.withDayOfMonth(10));
            createTransaction(1L, 1L, com.team31.financetracker.transaction.Enums.TransactionType.INCOME,
                    com.team31.financetracker.transaction.Enums.TransactionCategory.SALARY,
                    com.team31.financetracker.transaction.Enums.TransactionStatus.COMPLETED,
                    3000.0, baseDate.withDayOfMonth(15));
            createTransaction(1L, 1L, com.team31.financetracker.transaction.Enums.TransactionType.EXPENSE,
                    com.team31.financetracker.transaction.Enums.TransactionCategory.FOOD,
                    com.team31.financetracker.transaction.Enums.TransactionStatus.COMPLETED,
                    2000.0, baseDate.withDayOfMonth(7));
            createTransaction(1L, 1L, com.team31.financetracker.transaction.Enums.TransactionType.EXPENSE,
                    com.team31.financetracker.transaction.Enums.TransactionCategory.FOOD,
                    com.team31.financetracker.transaction.Enums.TransactionStatus.COMPLETED,
                    2000.0, baseDate.withDayOfMonth(20));
            createTransaction(1L, 1L, com.team31.financetracker.transaction.Enums.TransactionType.EXPENSE,
                    com.team31.financetracker.transaction.Enums.TransactionCategory.FOOD,
                    com.team31.financetracker.transaction.Enums.TransactionStatus.VOIDED,
                    1500.0, baseDate.withDayOfMonth(11));
            createTransaction(1L, 1L, com.team31.financetracker.transaction.Enums.TransactionType.EXPENSE,
                    com.team31.financetracker.transaction.Enums.TransactionCategory.FOOD,
                    com.team31.financetracker.transaction.Enums.TransactionStatus.VOIDED,
                    1500.0, baseDate.withDayOfMonth(16));
            createTransaction(1L, 1L, com.team31.financetracker.transaction.Enums.TransactionType.INCOME,
                    com.team31.financetracker.transaction.Enums.TransactionCategory.SALARY,
                    com.team31.financetracker.transaction.Enums.TransactionStatus.PENDING,
                    500.0, baseDate.withDayOfMonth(22));
            createTransaction(1L, 1L, com.team31.financetracker.transaction.Enums.TransactionType.EXPENSE,
                    com.team31.financetracker.transaction.Enums.TransactionCategory.RENT,
                    com.team31.financetracker.transaction.Enums.TransactionStatus.PENDING,
                    1000.0, baseDate.withDayOfMonth(25));

            org.mockito.Mockito.when(jwtService.isTokenValid(org.mockito.Mockito.anyString())).thenReturn(true);
            org.mockito.Mockito.when(jwtService.extractEmail(org.mockito.Mockito.anyString())).thenReturn("user@example.com");
            org.mockito.Mockito.when(jwtService.extractRole(org.mockito.Mockito.anyString())).thenReturn("USER");
            org.mockito.Mockito.when(jwtService.extractUserId(org.mockito.Mockito.anyString())).thenReturn(1L);

            mockMvc.perform(get("/api/transactions/analytics/dashboard")
                    .param("startDate", "2026-03-01")
                    .param("endDate", "2026-03-31")
                    .header("Authorization", "Bearer mock-token")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalTransactions").value(10))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalIncome").value(12000.0))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalExpenses").value(4000.0))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.netFlow").value(8000.0))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.completionRate").value(0.6))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.transactionsByStatus.COMPLETED").value(6))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.transactionsByStatus.VOIDED").value(2))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.transactionsByStatus.PENDING").value(2))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.transactionsByCategory.SALARY").value(5))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.transactionsByCategory.FOOD").value(4))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.transactionsByCategory.RENT").value(1));

            var events = transactionEventRepository.findAll();
            assertThat(events).isNotEmpty();
            assertThat(events.stream().anyMatch(event -> "ANALYTICS_VIEWED".equals(event.getAction()))).isTrue();
        }

        @Test
        @DisplayName("M2: Dashboard analytics returns 400 when date range is invalid")
        void testDashboardAnalyticsInvalidDateRange() throws Exception {
            org.mockito.Mockito.when(jwtService.isTokenValid(org.mockito.Mockito.anyString())).thenReturn(true);
            org.mockito.Mockito.when(jwtService.extractEmail(org.mockito.Mockito.anyString())).thenReturn("user@example.com");
            org.mockito.Mockito.when(jwtService.extractRole(org.mockito.Mockito.anyString())).thenReturn("USER");
            org.mockito.Mockito.when(jwtService.extractUserId(org.mockito.Mockito.anyString())).thenReturn(1L);

            mockMvc.perform(get("/api/transactions/analytics/dashboard")
                    .param("startDate", "2026-04-01")
                    .param("endDate", "2026-03-01")
                    .header("Authorization", "Bearer mock-token")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

/*
        @Test
        @DisplayName("Section 4.5: Verify Observer notification on Approve")
        void testObserverWiringOnApprove() throws Exception {
            // Setup a pending transaction
            com.team31.financetracker.transaction.model.Transaction tx = new com.team31.financetracker.transaction.model.Transaction();
            tx.setAccountId(1L);
            tx.setUserId(1L);
            tx.setType(com.team31.financetracker.transaction.Enums.TransactionType.EXPENSE);
            tx.setAmount(100.0);
            tx.setCurrency("USD");
            tx.setCategory(com.team31.financetracker.transaction.Enums.TransactionCategory.FOOD);
            tx.setStatus(com.team31.financetracker.transaction.Enums.TransactionStatus.PENDING);
            tx.setTransactionDate(LocalDateTime.now());

            com.team31.financetracker.transaction.model.Transaction saved = transactionRepository.save(tx);

            // Mock JWT validation
            org.mockito.Mockito.when(jwtService.isTokenValid(org.mockito.Mockito.anyString())).thenReturn(true);
            org.mockito.Mockito.when(jwtService.extractEmail(org.mockito.Mockito.anyString())).thenReturn("admin@example.com");
            org.mockito.Mockito.when(jwtService.extractRole(org.mockito.Mockito.anyString())).thenReturn("ADMIN");
            org.mockito.Mockito.when(jwtService.extractUserId(org.mockito.Mockito.anyString())).thenReturn(1L);

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/transactions/{transactionId}/approve", saved.getId())
                    .param("approverId", "1")
                    .header("Authorization", "Bearer mock-token")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // Verify event in MongoDB
            java.util.List<com.team31.financetracker.transaction.mongodb.TransactionEvent> events =
                transactionEventRepository.findByTransactionIdOrderByTimestampDesc(saved.getId());
            assertThat(events).isNotEmpty();
            assertThat(events.get(0).getAction()).isEqualTo("APPROVED");
        }
*/
    }

    private void createTransaction(Long accountId,
                                   Long userId,
                                   com.team31.financetracker.transaction.Enums.TransactionType type,
                                   com.team31.financetracker.transaction.Enums.TransactionCategory category,
                                   com.team31.financetracker.transaction.Enums.TransactionStatus status,
                                   Double amount,
                                   java.time.LocalDateTime transactionDate) {
        com.team31.financetracker.transaction.model.Transaction tx =
                new com.team31.financetracker.transaction.model.Transaction();
        tx.setAccountId(accountId);
        tx.setUserId(userId);
        tx.setType(type);
        tx.setCategory(category);
        tx.setStatus(status);
        tx.setAmount(amount);
        tx.setCurrency("USD");
        tx.setTransactionDate(transactionDate);
        transactionRepository.save(tx);
    }
}
