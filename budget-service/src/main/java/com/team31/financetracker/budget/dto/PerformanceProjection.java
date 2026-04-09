package com.team31.financetracker.budget.dto;

public interface PerformanceProjection {
    Integer getTotalBudgets();
    Double getTotalBudgeted();
    Double getTotalSpent();
    Double getAverageUtilization();
    Integer getExceededCount();
}
