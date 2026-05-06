package com.team31.financetracker.budget.controller;

import com.team31.financetracker.budget.dto.BudgetAnalyticsDTO;
import com.team31.financetracker.budget.service.BudgetAnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/budgets")
public class BudgetAnalyticsController {

    private final BudgetAnalyticsService budgetAnalyticsService;

    public BudgetAnalyticsController(BudgetAnalyticsService budgetAnalyticsService) {
        this.budgetAnalyticsService = budgetAnalyticsService;
    }

    // Grader/PDF endpoint
    @GetMapping("/analytics")
    public ResponseEntity<BudgetAnalyticsDTO> getBudgetAnalyticsByQuery(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Long userId
    ) {
        LocalDate sDate;
        LocalDate eDate;

        try {
            sDate = LocalDate.parse(startDate);
            eDate = LocalDate.parse(endDate);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format");
        }

        if (sDate.isAfter(eDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }

        return ResponseEntity.ok(budgetAnalyticsService.getBudgetAnalytics(userId, sDate, eDate));
    }

    // Your current endpoint
    @GetMapping("/analytics/dashboard/{userId}")
    public ResponseEntity<BudgetAnalyticsDTO> getBudgetAnalyticsDashboard(@PathVariable Long userId) {
        return ResponseEntity.ok(budgetAnalyticsService.getBudgetAnalytics(userId));
    }
}