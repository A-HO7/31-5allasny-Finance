package com.team31.financetracker.budget.dto;

import java.util.List;

public class BatchBudgetCreateRequest {

    private Long userId;
    private List<BatchBudgetCreateItem> budgets;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<BatchBudgetCreateItem> getBudgets() {
        return budgets;
    }

    public void setBudgets(List<BatchBudgetCreateItem> budgets) {
        this.budgets = budgets;
    }
}