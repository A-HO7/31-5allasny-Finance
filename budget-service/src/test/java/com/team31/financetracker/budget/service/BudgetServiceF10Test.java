package com.team31.financetracker.budget.service;

import com.team31.financetracker.budget.adapter.CassandraRowAdapter;
import com.team31.financetracker.budget.observer.MongoEventLogger;
import com.team31.financetracker.budget.repository.BudgetRepository;
import com.team31.financetracker.budget.repository.BudgetUsageEventRepository;
import com.team31.financetracker.contracts.feign.UserServiceClient;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceF10Test {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private BudgetUsageEventRepository budgetUsageEventRepository;
    @Mock
    private MongoEventLogger mongoEventLogger;
    @Mock
    private UserServiceClient userServiceClient;

    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(budgetRepository, budgetUsageEventRepository, mongoEventLogger, userServiceClient);
    }

    @Test
    void verifyUserExists_WhenUserFound_DoesNothing() {
        // Arrange
        Long userId = 1L;
        when(userServiceClient.getUser(userId)).thenReturn(null); // Just needs to not throw

        // Act & Assert
        budgetService.verifyUserExists(userId);
        verify(userServiceClient).getUser(userId);
    }

    @Test
    void verifyUserExists_WhenUserNotFound_Throws404() {
        // Arrange
        Long userId = 99L;
        Request request = Request.create(Request.HttpMethod.GET, "/api/users/" + userId, new HashMap<>(), null, new RequestTemplate());
        when(userServiceClient.getUser(userId)).thenThrow(new FeignException.NotFound("Not Found", request, null, null));

        // Act
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> budgetService.verifyUserExists(userId));

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found: 99", ex.getReason());
    }

    @Test
    void verifyUserExists_WhenUserServiceUnavailable_Throws503() {
        // Arrange
        Long userId = 1L;
        Request request = Request.create(Request.HttpMethod.GET, "/api/users/" + userId, new HashMap<>(), null, new RequestTemplate());
        when(userServiceClient.getUser(userId)).thenThrow(new FeignException.ServiceUnavailable("Service Unavailable", request, null, null));

        // Act
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> budgetService.verifyUserExists(userId));

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertEquals("User service unavailable", ex.getReason());
    }
}
