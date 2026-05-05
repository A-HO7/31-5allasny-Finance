package com.team31.financetracker.transaction.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team31.financetracker.transaction.dto.CategoryRecommendationDTO;
import com.team31.financetracker.transaction.observer.MongoEventLogger;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import com.team31.financetracker.transaction.repository.UserNodeRepository;
import com.team31.financetracker.transaction.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class TransactionRecommendationIntegrationTest {

    private static final String VALID_TOKEN = "recommendation-token";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private JwtService jwtService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private JdbcTemplate jdbcTemplate;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private TransactionRepository transactionRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private UserNodeRepository userNodeRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private MongoEventLogger mongoEventLogger;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        clearCaches();
    }

    @AfterEach
    void tearDown() {
        clearCaches();
    }

    @Test
    void recommendationsForOwnUserReturnExpectedCategories() throws Exception {
        mockAuthenticatedUser(1L, "USER", "a@example.com");
        mockUserExists(1L, true);
        when(userNodeRepository.getCategoryRecommendations(1L, 5)).thenReturn(List.of(
                recommendationRow("RENT", "EXPENSE_CATEGORY", 1, 80.0),
                recommendationRow("ENTERTAINMENT", "EXPENSE_CATEGORY", 1, 60.0)));

        List<CategoryRecommendationDTO> recommendations = performRecommendations(1L, null, null);

        assertThat(recommendations).extracting(CategoryRecommendationDTO::category)
                .containsExactly("RENT", "ENTERTAINMENT");
        assertThat(recommendations).extracting(CategoryRecommendationDTO::category)
                .doesNotContain("FOOD", "TRANSPORT");
        assertThat(recommendations).allSatisfy(dto -> assertThat(dto.score()).isEqualTo(1));
        assertThat(recommendations).extracting(CategoryRecommendationDTO::averageAmount)
                .containsExactly(80.0, 60.0);
    }

    @Test
    void recommendationsRespectCategoryTypeFilter() throws Exception {
        mockAuthenticatedUser(1L, "USER", "a@example.com");
        mockUserExists(1L, true);
        when(userNodeRepository.getCategoryRecommendations(1L, 5)).thenReturn(List.of(
                recommendationRow("RENT", "EXPENSE_CATEGORY", 1, 80.0),
                recommendationRow("ENTERTAINMENT", "EXPENSE_CATEGORY", 1, 60.0)));

        List<CategoryRecommendationDTO> recommendations = performRecommendations(1L, null, "EXPENSE_CATEGORY");

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations).allSatisfy(dto -> assertThat(dto.categoryType()).isEqualTo("EXPENSE_CATEGORY"));
    }

    @Test
    void recommendationsRespectLimitParameter() throws Exception {
        mockAuthenticatedUser(1L, "USER", "a@example.com");
        mockUserExists(1L, true);
        when(userNodeRepository.getCategoryRecommendations(1L, 1)).thenReturn(List.of(
                recommendationRow("RENT", "EXPENSE_CATEGORY", 1, 80.0)));

        List<CategoryRecommendationDTO> recommendations = performRecommendations(1L, 1, null);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).category()).isEqualTo("RENT");
        verify(userNodeRepository).getCategoryRecommendations(1L, 1);
    }

    @Test
    void recommendationsForAnotherUsersTokenReturnForbidden() throws Exception {
        mockAuthenticatedUser(2L, "USER", "b@example.com");

        mockMvc.perform(get("/api/transactions/recommendations")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .param("userId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recommendationsForAdminTokenBypassOwnershipCheck() throws Exception {
        mockAuthenticatedUser(99L, "ADMIN", "admin@example.com");
        mockUserExists(1L, true);
        when(userNodeRepository.getCategoryRecommendations(1L, 5)).thenReturn(List.of(
                recommendationRow("RENT", "EXPENSE_CATEGORY", 1, 80.0)));

        mockMvc.perform(get("/api/transactions/recommendations")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void recommendationsForUserWithoutPatternsReturnEmptyList() throws Exception {
        mockAuthenticatedUser(4L, "USER", "d@example.com");
        mockUserExists(4L, true);
        when(userNodeRepository.getCategoryRecommendations(4L, 5)).thenReturn(List.of());

        List<CategoryRecommendationDTO> recommendations = performRecommendations(4L, null, null);

        assertThat(recommendations).isEmpty();
    }

    @Test
    void recommendationsForMissingUserReturnNotFound() throws Exception {
        mockAuthenticatedUser(99L, "ADMIN", "admin@example.com");
        mockUserExists(999L, false);

        mockMvc.perform(get("/api/transactions/recommendations")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .param("userId", "999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void recommendationsWithoutTokenReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/transactions/recommendations")
                .param("userId", "1"))
                .andExpect(status().isUnauthorized());
    }

    private List<CategoryRecommendationDTO> performRecommendations(Long userId, Integer limit, String categoryType)
            throws Exception {
        var request = get("/api/transactions/recommendations")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .param("userId", userId.toString());

        if (limit != null) {
            request = request.param("limit", limit.toString());
        }
        if (categoryType != null) {
            request = request.param("categoryType", categoryType);
        }

        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(body, new TypeReference<List<CategoryRecommendationDTO>>() {
        });
    }

    private void mockAuthenticatedUser(Long userId, String role, String email) {
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId))
                .thenReturn(1);
        when(jwtService.isTokenValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(userId);
        when(jwtService.extractRole(anyString())).thenReturn(role);
        when(jwtService.extractEmail(anyString())).thenReturn(email);
    }

    private void mockUserExists(Long userId, boolean exists) {
        when(transactionRepository.existsUserById(userId)).thenReturn(exists);
    }

    private Map<String, Object> recommendationRow(String category, String categoryType, int score,
            double averageAmount) {
        return Map.of(
                "category", category,
                "categoryType", categoryType,
                "score", score,
                "averageAmount", averageAmount);
    }

    private void clearCaches() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}