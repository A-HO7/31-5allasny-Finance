package com.team31.financetracker.budget.dto;

public interface OverspentBudgetProjection {
    Long getBudgetId();
    Long getUserId();
    String getUserName();
    String getCategory();
    Double getBudgetAmount();
    Double getSpentAmount();
    Double getOverspendPercentage();
    Boolean getWarningSent();
}
