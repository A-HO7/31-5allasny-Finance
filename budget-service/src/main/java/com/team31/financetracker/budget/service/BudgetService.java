package com.team31.financetracker.budget.service;

import com.team31.financetracker.budget.dto.BatchBudgetCreateItem;
import com.team31.financetracker.budget.dto.BatchBudgetCreateRequest;
import com.team31.financetracker.budget.dto.BatchBudgetCreateResponse;
import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.BudgetStatus;
import com.team31.financetracker.budget.model.Category;
import com.team31.financetracker.budget.repository.BudgetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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

    @Transactional
    public BatchBudgetCreateResponse createBudgetsBatch(BatchBudgetCreateRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        if (request.getBudgets() == null || request.getBudgets().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "budgets list must not be empty");
        }

        boolean userExists = budgetRepository.existsUserById(request.getUserId());
        if (!userExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Budget> budgetsToSave = new ArrayList<>();
        int index = 0;
        for (BatchBudgetCreateItem item : request.getBudgets()) {
            index++;

            if (item.getBudgetAmount() == null || item.getBudgetAmount() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Budget amount must be positive for item " + index
                );
            }

            if (item.getStartDate() == null || item.getEndDate() == null || !item.getStartDate().isBefore(item.getEndDate())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "startDate must be before endDate for item " + index
                );
            }

            Budget budget = new Budget();
            budget.setUserId(request.getUserId());
            budget.setCategory(item.getCategory());
            budget.setBudgetAmount(item.getBudgetAmount());
            budget.setSpentAmount(0.0);
            budget.setPeriod(item.getPeriod());
            budget.setStartDate(item.getStartDate());
            budget.setEndDate(item.getEndDate());
            budget.setStatus(BudgetStatus.ACTIVE);
            budget.setMetadata(item.getMetadata() != null ? item.getMetadata() : new HashMap<>());

            budgetsToSave.add(budget);
        }

        List<Budget> savedBudgets = budgetRepository.saveAll(budgetsToSave);
        return new BatchBudgetCreateResponse(savedBudgets.size());
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

    public List<Budget> getBudgetHistory(LocalDate startDate, LocalDate endDate, Category category) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTimeExclusive = endDate.plusDays(1).atStartOfDay();

        return budgetRepository.findHistoryByCreatedAtRange(startDateTime, endDateTimeExclusive, category);
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