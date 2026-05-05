package com.team31.financetracker.budget.controller;

import com.team31.financetracker.budget.dto.BudgetAnalyticsDTO;
import com.team31.financetracker.budget.service.BudgetAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
public class BudgetAnalyticsController {

    private final BudgetAnalyticsService budgetAnalyticsService;

    public BudgetAnalyticsController(BudgetAnalyticsService budgetAnalyticsService) {
        this.budgetAnalyticsService = budgetAnalyticsService;
    }

    @GetMapping("/analytics/dashboard/{userId}")
    public ResponseEntity<BudgetAnalyticsDTO> getBudgetAnalytics(@PathVariable Long userId) {
        return ResponseEntity.ok(budgetAnalyticsService.getBudgetAnalytics(userId));
    }
}