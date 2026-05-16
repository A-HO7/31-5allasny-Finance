package com.team31.financetracker.budget.dto;

public interface BudgetSummaryProjection {
    Double getTotalBudgeted();
    Double getTotalSpent();
    Double getWeightedAdherenceRate();
}
