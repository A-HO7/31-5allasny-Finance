package com.team31.financetracker.budget.controller;

import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.Category;
import com.team31.financetracker.budget.service.BudgetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Budget createBudget(@RequestBody Budget budget) {
        return budgetService.createBudget(budget);
    }

    @GetMapping
    public List<Budget> getAllBudgets() {
        return budgetService.getAllBudgets();
    }

    @GetMapping("/metadata/search")
    public List<Budget> searchBudgetsByMetadata(
            @RequestParam String key,
            @RequestParam String operator,
            @RequestParam String value
    ) {
        return budgetService.searchBudgetsByMetadata(key, operator, value);
    }

    @GetMapping("/{id}")
    public Budget getBudgetById(@PathVariable Long id) {
        return budgetService.getBudgetById(id);
    }

    @PutMapping("/{id}")
    public Budget updateBudget(@PathVariable Long id, @RequestBody Budget budget) {
        return budgetService.updateBudget(id, budget);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
    }

    @GetMapping("/user/{userId}/active")
    public Budget getActiveBudgetForUserByCategory(
            @PathVariable Long userId,
            @RequestParam Category category
    ) {
        return budgetService.getActiveBudgetForUserByCategory(userId, category);
    }
}