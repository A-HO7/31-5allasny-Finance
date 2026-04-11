package com.team31.financetracker.budget.dto;

import com.team31.financetracker.budget.model.Category;

public class BudgetAlertDTO {
    private Long budgetId;
    private String userName;
    private Category category;
    private Double budgetAmount;
    private Double spentAmount;
    private Double percentUsed;
    private Double remainingAmount;

    public BudgetAlertDTO() {
    }

    public BudgetAlertDTO(Long budgetId, String userName, Category category,
                          Double budgetAmount, Double spentAmount,
                          Double percentUsed, Double remainingAmount) {
        this.budgetId = budgetId;
        this.userName = userName;
        this.category = category;
        this.budgetAmount = budgetAmount;
        this.spentAmount = spentAmount;
        this.percentUsed = percentUsed;
        this.remainingAmount = remainingAmount;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Double getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(Double budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public Double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(Double spentAmount) {
        this.spentAmount = spentAmount;
    }

    public Double getPercentUsed() {
        return percentUsed;
    }

    public void setPercentUsed(Double percentUsed) {
        this.percentUsed = percentUsed;
    }

    public Double getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(Double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }
}