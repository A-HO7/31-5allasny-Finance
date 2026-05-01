package com.team31.financetracker.budget.dto;

public interface BudgetAnalyticsProjection {

    Long getTotalBudgets();

    Double getTotalBudgetAmount();

    Double getTotalSpentAmount();

    Double getAverageUtilization();

    Long getActiveBudgets();

    Long getExceededBudgets();

    Long getCompletedBudgets();
}