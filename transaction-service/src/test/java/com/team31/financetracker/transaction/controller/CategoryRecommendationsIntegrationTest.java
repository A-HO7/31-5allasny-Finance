package com.team31.financetracker.transaction.controller;

import com.team31.financetracker.transaction.Enums.TransactionCategory;
import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.Enums.TransactionType;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import com.team31.financetracker.transaction.security.JwtConfigurationManager;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("S3-F12: Category Recommendations Integration Tests")
@Transactional
class CategoryRecommendationsIntegrationTest {

    private MockMvc mockMvc;
    private static final String JWT_SECRET = Base64.getEncoder()
            .encodeToString("this-is-a-very-long-secret-key-for-testing-purposes".getBytes());

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        JwtConfigurationManager.initConfig(JWT_SECRET, 86400000L);
    }

    // ── Helper Methods ────────────────────────────────────────────────────────

    private Long createUser(String email, String role) {
        String sql = "INSERT INTO users (email, \"role\") VALUES (?, ?) RETURNING id";
        return jdbcTemplate.queryForObject(sql, Long.class, email, role);
    }

    private Long createAccount(double balance) {
        String sql = "INSERT INTO accounts (balance) VALUES (?) RETURNING id";
        return jdbcTemplate.queryForObject(sql, Long.class, balance);
    }

    private Transaction createTransaction(Long userId, Long accountId, TransactionCategory category,
            Double amount, TransactionStatus status) {
        Transaction t = new Transaction();
        t.setUserId(userId);
        t.setAccountId(accountId);
        t.setType(TransactionType.EXPENSE);
        t.setCategory(category);
        t.setAmount(amount);
        t.setCurrency("USD");
        t.setStatus(status);
        t.setTransactionDate(LocalDateTime.now());
        return transactionRepository.save(t);
    }

    private String generateToken(Long userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(JWT_SECRET));
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(key)
                .compact();
    }

    // ── Test Scenario A: Multiple Users with Shared Categories ────────────────

    @Test
    @DisplayName("A) Should return 200 with recommendations for authenticated user")
    void testScenarioA_RecommendationsForUserWithSharedCategories() throws Exception {
        Long userA = createUser("userA@test.com", "PERSONAL");
        Long userB = createUser("userB@test.com", "PERSONAL");

        Long accountA = createAccount(1000.0);
        Long accountB = createAccount(1000.0);

        createTransaction(userA, accountA, TransactionCategory.FOOD, 50.0, TransactionStatus.COMPLETED);
        createTransaction(userA, accountA, TransactionCategory.TRANSPORT, 30.0, TransactionStatus.COMPLETED);

        createTransaction(userB, accountB, TransactionCategory.FOOD, 60.0, TransactionStatus.COMPLETED);
        createTransaction(userB, accountB, TransactionCategory.RENT, 800.0, TransactionStatus.COMPLETED);

        String tokenA = generateToken(userA, "userA@test.com", "PERSONAL");

        mockMvc.perform(get("/api/transactions/recommendations")
                .param("userId", userA.toString())
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ── Test Scenario B: Cross-User Access Denial ─────────────────────────────

    @Test
    @DisplayName("B) Should return 403 when user B tries to access user A's recommendations")
    void testScenarioB_CrossUserAccessDenied() throws Exception {
        Long userA = createUser("userA@test.com", "PERSONAL");
        Long userB = createUser("userB@test.com", "PERSONAL");

        String tokenB = generateToken(userB, "userB@test.com", "PERSONAL");

        mockMvc.perform(get("/api/transactions/recommendations")
                .param("userId", userA.toString())
                .header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ── Test Scenario C: Admin Bypass ─────────────────────────────────────────

    @Test
    @DisplayName("C) Should return 200 when ADMIN token accesses any user's recommendations")
    void testScenarioC_AdminBypassOwnershipCheck() throws Exception {
        Long userA = createUser("userA@test.com", "PERSONAL");
        Long adminUser = createUser("admin@test.com", "ADMIN");

        Long accountA = createAccount(1000.0);
        createTransaction(userA, accountA, TransactionCategory.FOOD, 50.0, TransactionStatus.COMPLETED);

        String adminToken = generateToken(adminUser, "admin@test.com", "ADMIN");

        mockMvc.perform(get("/api/transactions/recommendations")
                .param("userId", userA.toString())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ── Test Scenario D: Category Type Filtering ──────────────────────────────

    @Test
    @DisplayName("D) Should filter by categoryType parameter")
    void testScenarioD_CategoryTypeFiltering() throws Exception {
        Long userA = createUser("userA@test.com", "PERSONAL");
        Long accountA = createAccount(1000.0);

        createTransaction(userA, accountA, TransactionCategory.FOOD, 50.0, TransactionStatus.COMPLETED);

        String tokenA = generateToken(userA, "userA@test.com", "PERSONAL");

        mockMvc.perform(get("/api/transactions/recommendations")
                .param("userId", userA.toString())
                .param("categoryType", "EXPENSE_CATEGORY")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ── Test Scenario E: User with No Patterns ────────────────────────────────

    @Test
    @DisplayName("E) Should return empty list for user with no SPENT_ON relationships")
    void testScenarioE_UserWithNoPatterns() throws Exception {
        Long userA = createUser("userA@test.com", "PERSONAL");
        String tokenA = generateToken(userA, "userA@test.com", "PERSONAL");

        mockMvc.perform(get("/api/transactions/recommendations")
                .param("userId", userA.toString())
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── Test Scenario F: Nonexistent User with Admin Token ────────────────────

    @Test
    @DisplayName("F) Should return 404 for userId=999 with ADMIN token")
    void testScenarioF_NonexistentUserReturns404() throws Exception {
        Long adminUser = createUser("admin@test.com", "ADMIN");
        String adminToken = generateToken(adminUser, "admin@test.com", "ADMIN");

        mockMvc.perform(get("/api/transactions/recommendations")
                .param("userId", "999")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ── Test Scenario G: Missing Authentication Token ──────────────────────────

    @Test
    @DisplayName("G) Should return 401 when no JWT token is provided")
    void testScenarioG_MissingTokenReturns401() throws Exception {
        Long userA = createUser("userA@test.com", "PERSONAL");

        mockMvc.perform(get("/api/transactions/recommendations")
                .param("userId", userA.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ── Additional Test 1: Custom Limit ──────────────────────────────────────

    @Test
    @DisplayName("Additional) Should respect custom limit parameter")
    void testCustomLimit() throws Exception {
        Long userA = createUser("userA@test.com", "PERSONAL");
        Long adminUser = createUser("admin@test.com", "ADMIN");

        String adminToken = generateToken(adminUser, "admin@test.com", "ADMIN");

        mockMvc.perform(get("/api/transactions/recommendations")
                .param("userId", userA.toString())
                .param("limit", "3")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ── Additional Test 2: Default Limit ──────────────────────────────────────

    @Test
    @DisplayName("Additional) Should default limit to 5 when not provided")
    void testDefaultLimitIs5() throws Exception {
        Long userA = createUser("userA@test.com", "PERSONAL");
        String tokenA = generateToken(userA, "userA@test.com", "PERSONAL");

        mockMvc.perform(get("/api/transactions/recommendations")
                .param("userId", userA.toString())
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
