package com.team31.financetracker.budget.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class BudgetAnalyticsDTO implements Serializable {

    private Long userId;
    private Long totalBudgets;
    private Double totalBudgetAmount;
    private Double totalSpentAmount;
    private Double remainingAmount;
    private Double averageUtilization;
    private Long activeBudgets;
    private Long overspentBudgets;
    private Long completedBudgets;
    private java.util.Map<String, Long> budgetsByStatus;
    private java.util.Map<String, Long> budgetsByPeriod;

    private BudgetAnalyticsDTO(Builder builder) {
        this.userId = builder.userId;
        this.totalBudgets = builder.totalBudgets;
        this.totalBudgetAmount = builder.totalBudgetAmount;
        this.totalSpentAmount = builder.totalSpentAmount;
        this.remainingAmount = builder.remainingAmount;
        this.averageUtilization = builder.averageUtilization;
        this.activeBudgets = builder.activeBudgets;
        this.overspentBudgets = builder.overspentBudgets;
        this.completedBudgets = builder.completedBudgets;
        this.budgetsByStatus = builder.budgetsByStatus != null ? builder.budgetsByStatus : new java.util.HashMap<>();
        this.budgetsByPeriod = builder.budgetsByPeriod != null ? builder.budgetsByPeriod : new java.util.HashMap<>();
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

    @JsonIgnore
    public Double getTotalBudgetAmount() {
        return totalBudgetAmount;
    }

    @JsonProperty("totalBudgeted")
    public Double getTotalBudgeted() {
        return totalBudgetAmount;
    }

    @JsonIgnore
    public Double getTotalSpentAmount() {
        return totalSpentAmount;
    }

    @JsonProperty("totalSpent")
    public Double getTotalSpent() {
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

    public Long getOverspentBudgets() {
        return overspentBudgets;
    }

    public Long getCompletedBudgets() {
        return completedBudgets;
    }

    public java.util.Map<String, Long> getBudgetsByStatus() {
        return budgetsByStatus;
    }

    public java.util.Map<String, Long> getBudgetsByPeriod() {
        return budgetsByPeriod;
    }

    public static class Builder {
        private Long userId;
        private Long totalBudgets;
        private Double totalBudgetAmount;
        private Double totalSpentAmount;
        private Double remainingAmount;
        private Double averageUtilization;
        private Long activeBudgets;
        private Long overspentBudgets;
        private Long completedBudgets;
        private java.util.Map<String, Long> budgetsByStatus;
        private java.util.Map<String, Long> budgetsByPeriod;

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

        public Builder overspentBudgets(Long overspentBudgets) {
            this.overspentBudgets = overspentBudgets;
            return this;
        }

        public Builder completedBudgets(Long completedBudgets) {
            this.completedBudgets = completedBudgets;
            return this;
        }

        public Builder budgetsByStatus(java.util.Map<String, Long> budgetsByStatus) {
            this.budgetsByStatus = budgetsByStatus;
            return this;
        }

        public Builder budgetsByPeriod(java.util.Map<String, Long> budgetsByPeriod) {
            this.budgetsByPeriod = budgetsByPeriod;
            return this;
        }

        public BudgetAnalyticsDTO build() {
            return new BudgetAnalyticsDTO(this);
        }
    }
}