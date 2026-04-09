package com.team31.financetracker.budget.controller;

import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.Category;
import com.team31.financetracker.budget.service.BudgetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/user/{userId}/active")
    public Budget getActiveBudgetForUserByCategory(
            @PathVariable Long userId,
            @RequestParam Category category
    ) {
        return budgetService.getActiveBudgetForUserByCategory(userId, category);
    }
}