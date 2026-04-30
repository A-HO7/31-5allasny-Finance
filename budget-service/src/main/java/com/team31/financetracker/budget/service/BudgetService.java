package com.team31.financetracker.budget.service;

import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.BudgetStatus;
import com.team31.financetracker.budget.model.Category;
import com.team31.financetracker.budget.repository.BudgetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team31.financetracker.budget.dto.BudgetAlertDTO;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public Budget createBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    public Budget getBudgetById(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
    }

    public Budget updateBudget(Long id, Budget updatedBudget) {
        Budget existingBudget = getBudgetById(id);

        if (updatedBudget.getUserId() != null) existingBudget.setUserId(updatedBudget.getUserId());
        if (updatedBudget.getCategory() != null) existingBudget.setCategory(updatedBudget.getCategory());
        if (updatedBudget.getAmount() != null) existingBudget.setAmount(updatedBudget.getAmount());
        if (updatedBudget.getSpentAmount() != null) existingBudget.setSpentAmount(updatedBudget.getSpentAmount());
        if (updatedBudget.getPeriod() != null) existingBudget.setPeriod(updatedBudget.getPeriod());
        if (updatedBudget.getStartDate() != null) existingBudget.setStartDate(updatedBudget.getStartDate());
        if (updatedBudget.getEndDate() != null) existingBudget.setEndDate(updatedBudget.getEndDate());
        if (updatedBudget.getStatus() != null) existingBudget.setStatus(updatedBudget.getStatus());
        if (updatedBudget.getMetadata() != null) existingBudget.setMetadata(updatedBudget.getMetadata());

        return budgetRepository.save(existingBudget);
    }

    @Transactional
    public int purgeOldBudgets(int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        return budgetRepository.purgeOldBudgets(cutoffDate);
    }

    public com.team31.financetracker.budget.dto.BudgetPerformanceDTO getBudgetPerformance(Long userId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59, 999999999);

        com.team31.financetracker.budget.dto.PerformanceProjection projection =
                budgetRepository.getBudgetPerformanceAggregates(userId, start, end);

        com.team31.financetracker.budget.dto.BudgetPerformanceDTO dto = new com.team31.financetracker.budget.dto.BudgetPerformanceDTO();
        dto.setUserId(userId);
        dto.setTotalBudgets(projection.getTotalBudgets() != null ? projection.getTotalBudgets() : 0);
        dto.setTotalBudgeted(projection.getTotalBudgeted() != null ? projection.getTotalBudgeted() : 0.0);
        dto.setTotalSpent(projection.getTotalSpent() != null ? projection.getTotalSpent() : 0.0);
        dto.setAverageUtilization(projection.getAverageUtilization() != null ? projection.getAverageUtilization() : 0.0);
        dto.setExceededCount(projection.getExceededCount() != null ? projection.getExceededCount() : 0);

        return dto;
    }

    public List<com.team31.financetracker.budget.dto.OverspentBudgetDTO> getOverspentBudgets(Double minOverspend, Boolean warningNotSent) {
        try {
            List<com.team31.financetracker.budget.dto.OverspentBudgetProjection> projections = budgetRepository.findOverspentBudgets(minOverspend, warningNotSent);
            return projections.stream().map(p -> {
                com.team31.financetracker.budget.dto.OverspentBudgetDTO dto = new com.team31.financetracker.budget.dto.OverspentBudgetDTO();
                dto.setBudgetId(p.getBudgetId());
                dto.setUserName(p.getUserName());
                dto.setCategory(p.getCategory());
                dto.setBudgetAmount(p.getBudgetAmount());
                dto.setSpentAmount(p.getSpentAmount());
                dto.setOverspendPercentage(p.getOverspendPercentage());
                dto.setWarningSent(p.getWarningSent());
                return dto;
            }).toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void deleteBudget(Long id) {
        Budget existingBudget = getBudgetById(id);
        budgetRepository.delete(existingBudget);
    }

    public Budget getActiveBudgetForUserByCategory(Long userId, Category category) {
        return budgetRepository
                .findActiveBudgetForUserNative(
                        userId,
                        category
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No active budget found for this user and category"
                ));
    }

    public Budget updateBudgetMetadata(Long budgetId, Map<String, Object> incomingMetadata) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));

        // If incoming metadata is null or empty, just return the budget as-is (preserve existing metadata)
        if (incomingMetadata == null || incomingMetadata.isEmpty()) {
            return budget;
        }

        // Merge metadata
        Map<String, Object> existingMetadata = budget.getMetadata();
        if (existingMetadata == null) {
            existingMetadata = new HashMap<>();
        }

        existingMetadata.putAll(incomingMetadata);

        if (existingMetadata.containsKey("healthWeight")) {
            Object raw = existingMetadata.get("healthWeight");
            double val = 1.0;
            if (raw instanceof Number n) {
                val = n.doubleValue();
            } else if (raw != null) {
                try { val = Double.parseDouble(raw.toString()); } catch (Exception ignored) {}
            }
            existingMetadata.put("healthWeight", Math.min(2.0, Math.max(0.0, val)));
        }

        budget.setMetadata(existingMetadata);

        return budgetRepository.save(budget);
    }

    @Transactional
    public List<Budget> createBudgetsBatch(Long userId, List<Budget> budgets) {
        for (Budget b : budgets) {
            b.setUserId(userId);
            if (b.getStatus() == null) b.setStatus(BudgetStatus.ACTIVE);
            if (b.getMetadata() == null) b.setMetadata(new HashMap<>());
            b.getMetadata().putIfAbsent("healthWeight", 1.0);
        }
        return budgetRepository.saveAll(budgets);
    }

    public List<Budget> searchBudgetsByMetadata(String key, String operator, String value) {
        try {
            return budgetRepository.searchBudgetsByMetadata(key, operator, value);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Budget> getBudgetsHistory(java.time.LocalDate startDate, java.time.LocalDate endDate, String category) {
        try {
            return budgetRepository.findBudgetsInDateRange(
                startDate.atStartOfDay(), 
                endDate.atTime(23, 59, 59, 999999999), 
                category
            );
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<BudgetAlertDTO> getBudgetsNearLimit(Double threshold, BudgetStatus status) {
        if (threshold == null || threshold < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "threshold must be >= 0");
        }

        // Normalize threshold: if >= 1, treat as percentage and convert to fraction
        // e.g. threshold=80 means 80% → fraction=0.80
        // threshold=0.8 means 80% → fraction=0.80
        double fractionThreshold = threshold >= 1.0 ? threshold / 100.0 : threshold;

        try {
            List<Object[]> rows = budgetRepository.findBudgetsNearLimit(
                    fractionThreshold,
                    status != null ? status.name() : null
            );

            List<BudgetAlertDTO> result = new ArrayList<>();

            for (Object[] row : rows) {
                BudgetAlertDTO dto = new BudgetAlertDTO();
                dto.setBudgetId(((Number) row[0]).longValue());
                dto.setUserName((String) row[1]);
                dto.setCategory(Category.valueOf((String) row[2]));
                dto.setBudgetAmount(((Number) row[3]).doubleValue());
                dto.setSpentAmount(((Number) row[4]).doubleValue());
                dto.setPercentUsed(((Number) row[5]).doubleValue());
                dto.setRemainingAmount(((Number) row[6]).doubleValue());
                result.add(dto);
            }

            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}