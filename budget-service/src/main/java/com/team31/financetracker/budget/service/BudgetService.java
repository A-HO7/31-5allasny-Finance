package com.team31.financetracker.budget.service;

import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.BudgetStatus;
import com.team31.financetracker.budget.model.Category;
import com.team31.financetracker.budget.repository.BudgetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

        existingBudget.setUserId(updatedBudget.getUserId());
        existingBudget.setCategory(updatedBudget.getCategory());
        existingBudget.setBudgetAmount(updatedBudget.getBudgetAmount());
        existingBudget.setSpentAmount(updatedBudget.getSpentAmount());
        existingBudget.setPeriod(updatedBudget.getPeriod());
        existingBudget.setStartDate(updatedBudget.getStartDate());
        existingBudget.setEndDate(updatedBudget.getEndDate());
        existingBudget.setStatus(updatedBudget.getStatus());
        existingBudget.setMetadata(updatedBudget.getMetadata());

        return budgetRepository.save(existingBudget);
    }

    @Transactional
    public int purgeOldBudgets(int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        return budgetRepository.purgeOldBudgets(cutoffDate);
    }

    public com.team31.financetracker.budget.dto.BudgetPerformanceDTO getBudgetPerformance(Long userId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (!budgetRepository.existsUserById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

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
    }

    public void deleteBudget(Long id) {
        Budget existingBudget = getBudgetById(id);
        budgetRepository.delete(existingBudget);
    }

    public Budget getActiveBudgetForUserByCategory(Long userId, Category category) {
        boolean userExists = budgetRepository.existsUserById(userId);

        if (!userExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        return budgetRepository
                .findFirstByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(
                        userId,
                        category,
                        BudgetStatus.ACTIVE
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No active budget found for this user and category"
                ));
    }
}