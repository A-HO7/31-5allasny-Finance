package com.team31.financetracker.budget.service;

import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.BudgetStatus;
import com.team31.financetracker.budget.model.Category;
import com.team31.financetracker.budget.repository.BudgetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public List<Budget> searchBudgetsByMetadata(String key, String operator, String value) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key must not be empty");
        }

        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "value must not be empty");
        }

        return switch (operator) {
            case "eq" -> budgetRepository.findByMetadataKeyEquals(key, value);
            case "gt" -> {
                validateNumericValue(value, operator);
                yield budgetRepository.findByMetadataKeyGreaterThan(key, value);
            }
            case "lt" -> {
                validateNumericValue(value, operator);
                yield budgetRepository.findByMetadataKeyLessThan(key, value);
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid operator. Allowed values: eq, gt, lt"
            );
        };
    }

    private void validateNumericValue(String value, String operator) {
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "value must be numeric when operator is " + operator
            );
        }
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
}