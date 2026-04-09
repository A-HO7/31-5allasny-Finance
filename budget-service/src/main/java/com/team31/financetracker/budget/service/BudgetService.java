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

    @Transactional
    public int purgeOldBudgets(int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        return budgetRepository.purgeOldBudgets(cutoffDate);
    }
}