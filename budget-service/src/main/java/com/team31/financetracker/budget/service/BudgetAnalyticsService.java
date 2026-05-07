package com.team31.financetracker.budget.service;

import com.team31.financetracker.budget.dto.BudgetAnalyticsDTO;
import com.team31.financetracker.budget.dto.BudgetAnalyticsProjection;
import com.team31.financetracker.budget.observer.BudgetSubject;
import com.team31.financetracker.budget.observer.EntityObserver;
import com.team31.financetracker.budget.observer.MongoEventLogger;
import com.team31.financetracker.budget.repository.BudgetRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetAnalyticsService implements BudgetSubject {

    private final BudgetRepository budgetRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final List<EntityObserver> observers = new ArrayList<>();

    public BudgetAnalyticsService(BudgetRepository budgetRepository,
                                  RedisTemplate<String, Object> redisTemplate,
                                  MongoEventLogger mongoEventLogger) {
        this.budgetRepository = budgetRepository;
        this.redisTemplate = redisTemplate;
        registerObserver(mongoEventLogger);
    }

    public BudgetAnalyticsDTO getBudgetAnalytics(Long userId) {
        String cacheKey = "budget-service::S4-F10::" + userId;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof BudgetAnalyticsDTO dto) {
                logDashboardViewed(userId, true);
                return dto;
            }
        } catch (Exception ignored) {
            // Redis is a soft dependency.
        }

        BudgetAnalyticsProjection projection = budgetRepository.getBudgetAnalytics(userId);
        List<Object[]> statusCounts = budgetRepository.countBudgetsByStatus(userId);
        List<Object[]> periodCounts = budgetRepository.countBudgetsByPeriod(userId);
        
        BudgetAnalyticsDTO dto = buildDto(userId, projection, statusCounts, periodCounts);

        try {
            redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofMinutes(10));
        } catch (Exception ignored) {
            // Redis is a soft dependency.
        }

        logDashboardViewed(userId, false);

        return dto;
    }

    public BudgetAnalyticsDTO getBudgetAnalytics(Long userId, LocalDate startDate, LocalDate endDate) {
        String cacheKey = "budget-service::S4-F10::" + userId + "::" + startDate + "::" + endDate;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof BudgetAnalyticsDTO dto) {
                logDashboardViewed(userId, true);
                return dto;
            }
        } catch (Exception ignored) {
            // Redis is a soft dependency.
        }

        BudgetAnalyticsProjection projection =
                budgetRepository.getBudgetAnalyticsByDateRange(userId, startDate, endDate);
                
        List<Object[]> statusCounts = budgetRepository.countBudgetsByStatusByDateRange(userId, startDate, endDate);
        List<Object[]> periodCounts = budgetRepository.countBudgetsByPeriodByDateRange(userId, startDate, endDate);

        BudgetAnalyticsDTO dto = buildDto(userId, projection, statusCounts, periodCounts);

        try {
            redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofMinutes(10));
        } catch (Exception ignored) {
            // Redis is a soft dependency.
        }

        logDashboardViewed(userId, false);

        return dto;
    }

    private BudgetAnalyticsDTO buildDto(Long userId, BudgetAnalyticsProjection projection, List<Object[]> statusCounts, List<Object[]> periodCounts) {
        long totalBudgets = projection != null && projection.getTotalBudgets() != null
                ? projection.getTotalBudgets()
                : 0L;

        double totalBudgetAmount = projection != null && projection.getTotalBudgetAmount() != null
                ? projection.getTotalBudgetAmount()
                : 0.0;

        double totalSpentAmount = projection != null && projection.getTotalSpentAmount() != null
                ? projection.getTotalSpentAmount()
                : 0.0;

        double averageUtilization = projection != null && projection.getAverageUtilization() != null
                ? projection.getAverageUtilization()
                : 0.0;

        long activeBudgets = projection != null && projection.getActiveBudgets() != null
                ? projection.getActiveBudgets()
                : 0L;

        long exceededBudgets = projection != null && projection.getExceededBudgets() != null
                ? projection.getExceededBudgets()
                : 0L;

        long completedBudgets = projection != null && projection.getCompletedBudgets() != null
                ? projection.getCompletedBudgets()
                : 0L;
                
        Map<String, Long> budgetsByStatus = new HashMap<>();
        if (statusCounts != null) {
            for (Object[] row : statusCounts) {
                if (row[0] != null) {
                    budgetsByStatus.put(row[0].toString(), ((Number) row[1]).longValue());
                }
            }
        }
        
        Map<String, Long> budgetsByPeriod = new HashMap<>();
        if (periodCounts != null) {
            for (Object[] row : periodCounts) {
                if (row[0] != null) {
                    budgetsByPeriod.put(row[0].toString(), ((Number) row[1]).longValue());
                }
            }
        }

        return BudgetAnalyticsDTO.builder()
                .userId(userId)
                .totalBudgets(totalBudgets)
                .totalBudgetAmount(totalBudgetAmount)
                .totalSpentAmount(totalSpentAmount)
                .remainingAmount(totalBudgetAmount - totalSpentAmount)
                .averageUtilization(averageUtilization)
                .activeBudgets(activeBudgets)
                .overspentBudgets(exceededBudgets)
                .completedBudgets(completedBudgets)
                .budgetsByStatus(budgetsByStatus)
                .budgetsByPeriod(budgetsByPeriod)
                .build();
    }

    private void logDashboardViewed(Long userId, boolean cacheHit) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("feature", "S4-F10");
        payload.put("cacheHit", cacheHit);

        notifyObservers("DASHBOARD_VIEWED", payload);
    }

    @Override
    public void registerObserver(EntityObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unregisterObserver(EntityObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }
}