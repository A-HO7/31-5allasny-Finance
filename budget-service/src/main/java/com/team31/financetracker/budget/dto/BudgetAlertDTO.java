package com.team31.financetracker.budget.dto;

import com.team31.financetracker.budget.model.Category;

import java.io.Serializable;

/**
 * DTO for budget-alert / near-limit responses.
 * Includes a static inner Builder (DP-4).
 */
public class BudgetAlertDTO implements Serializable {

    private Long budgetId;
    private String userName;
    private Category category;
    private Double budgetAmount;
    private Double spentAmount;
    private Double percentUsed;
    private Double remainingAmount;

    public BudgetAlertDTO() {}

    private BudgetAlertDTO(Builder builder) {
        this.budgetId = builder.budgetId;
        this.userName = builder.userName;
        this.category = builder.category;
        this.budgetAmount = builder.budgetAmount;
        this.spentAmount = builder.spentAmount;
        this.percentUsed = builder.percentUsed;
        this.remainingAmount = builder.remainingAmount;
    }

    // Getters & Setters

    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Double getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(Double budgetAmount) { this.budgetAmount = budgetAmount; }

    public Double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(Double spentAmount) { this.spentAmount = spentAmount; }

    public Double getPercentUsed() { return percentUsed; }
    public void setPercentUsed(Double percentUsed) { this.percentUsed = percentUsed; }

    public Double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Double remainingAmount) { this.remainingAmount = remainingAmount; }

    // ──── Builder (DP-4) ────

    public static class Builder {
        private Long budgetId;
        private String userName;
        private Category category;
        private Double budgetAmount;
        private Double spentAmount;
        private Double percentUsed;
        private Double remainingAmount;

        public Builder budgetId(Long budgetId) { this.budgetId = budgetId; return this; }
        public Builder userName(String userName) { this.userName = userName; return this; }
        public Builder category(Category category) { this.category = category; return this; }
        public Builder budgetAmount(Double budgetAmount) { this.budgetAmount = budgetAmount; return this; }
        public Builder spentAmount(Double spentAmount) { this.spentAmount = spentAmount; return this; }
        public Builder percentUsed(Double percentUsed) { this.percentUsed = percentUsed; return this; }
        public Builder remainingAmount(Double remainingAmount) { this.remainingAmount = remainingAmount; return this; }

        public BudgetAlertDTO build() {
            return new BudgetAlertDTO(this);
        }
    }
}