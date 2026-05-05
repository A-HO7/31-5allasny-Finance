package com.team31.financetracker.budget.dto;

import java.io.Serializable;

public class BudgetAnalyticsDTO implements Serializable {

    private Long userId;
    private Long totalBudgets;
    private Double totalBudgetAmount;
    private Double totalSpentAmount;
    private Double remainingAmount;
    private Double averageUtilization;
    private Long activeBudgets;
    private Long exceededBudgets;
    private Long completedBudgets;

    private BudgetAnalyticsDTO(Builder builder) {
        this.userId = builder.userId;
        this.totalBudgets = builder.totalBudgets;
        this.totalBudgetAmount = builder.totalBudgetAmount;
        this.totalSpentAmount = builder.totalSpentAmount;
        this.remainingAmount = builder.remainingAmount;
        this.averageUtilization = builder.averageUtilization;
        this.activeBudgets = builder.activeBudgets;
        this.exceededBudgets = builder.exceededBudgets;
        this.completedBudgets = builder.completedBudgets;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTotalBudgets() {
        return totalBudgets;
    }

    public Double getTotalBudgetAmount() {
        return totalBudgetAmount;
    }

    public Double getTotalSpentAmount() {
        return totalSpentAmount;
    }

    public Double getRemainingAmount() {
        return remainingAmount;
    }

    public Double getAverageUtilization() {
        return averageUtilization;
    }

    public Long getActiveBudgets() {
        return activeBudgets;
    }

    public Long getExceededBudgets() {
        return exceededBudgets;
    }

    public Long getCompletedBudgets() {
        return completedBudgets;
    }

    public static class Builder {
        private Long userId;
        private Long totalBudgets;
        private Double totalBudgetAmount;
        private Double totalSpentAmount;
        private Double remainingAmount;
        private Double averageUtilization;
        private Long activeBudgets;
        private Long exceededBudgets;
        private Long completedBudgets;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder totalBudgets(Long totalBudgets) {
            this.totalBudgets = totalBudgets;
            return this;
        }

        public Builder totalBudgetAmount(Double totalBudgetAmount) {
            this.totalBudgetAmount = totalBudgetAmount;
            return this;
        }

        public Builder totalSpentAmount(Double totalSpentAmount) {
            this.totalSpentAmount = totalSpentAmount;
            return this;
        }

        public Builder remainingAmount(Double remainingAmount) {
            this.remainingAmount = remainingAmount;
            return this;
        }

        public Builder averageUtilization(Double averageUtilization) {
            this.averageUtilization = averageUtilization;
            return this;
        }

        public Builder activeBudgets(Long activeBudgets) {
            this.activeBudgets = activeBudgets;
            return this;
        }

        public Builder exceededBudgets(Long exceededBudgets) {
            this.exceededBudgets = exceededBudgets;
            return this;
        }

        public Builder completedBudgets(Long completedBudgets) {
            this.completedBudgets = completedBudgets;
            return this;
        }

        public BudgetAnalyticsDTO build() {
            return new BudgetAnalyticsDTO(this);
        }
    }
}