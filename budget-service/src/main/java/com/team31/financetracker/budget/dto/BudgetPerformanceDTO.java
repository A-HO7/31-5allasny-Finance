package com.team31.financetracker.budget.dto;

import java.io.Serializable;

/**
 * DTO for budget performance summary.
 * Includes a static inner Builder (DP-4).
 */
public class BudgetPerformanceDTO implements Serializable {

    private Long userId;
    private Integer totalBudgets;
    private Double totalBudgeted;
    private Double totalSpent;
    private Double averageUtilization;
    private Integer exceededCount;

    public BudgetPerformanceDTO() {}

    private BudgetPerformanceDTO(Builder builder) {
        this.userId = builder.userId;
        this.totalBudgets = builder.totalBudgets;
        this.totalBudgeted = builder.totalBudgeted;
        this.totalSpent = builder.totalSpent;
        this.averageUtilization = builder.averageUtilization;
        this.exceededCount = builder.exceededCount;
    }

    // Getters & Setters

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getTotalBudgets() { return totalBudgets; }
    public void setTotalBudgets(Integer totalBudgets) { this.totalBudgets = totalBudgets; }

    public Double getTotalBudgeted() { return totalBudgeted; }
    public void setTotalBudgeted(Double totalBudgeted) { this.totalBudgeted = totalBudgeted; }

    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }

    public Double getAverageUtilization() { return averageUtilization; }
    public void setAverageUtilization(Double averageUtilization) { this.averageUtilization = averageUtilization; }

    public Integer getExceededCount() { return exceededCount; }
    public void setExceededCount(Integer exceededCount) { this.exceededCount = exceededCount; }

    // ──── Builder (DP-4) ────

    public static class Builder {
        private Long userId;
        private Integer totalBudgets;
        private Double totalBudgeted;
        private Double totalSpent;
        private Double averageUtilization;
        private Integer exceededCount;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder totalBudgets(Integer totalBudgets) { this.totalBudgets = totalBudgets; return this; }
        public Builder totalBudgeted(Double totalBudgeted) { this.totalBudgeted = totalBudgeted; return this; }
        public Builder totalSpent(Double totalSpent) { this.totalSpent = totalSpent; return this; }
        public Builder averageUtilization(Double averageUtilization) { this.averageUtilization = averageUtilization; return this; }
        public Builder exceededCount(Integer exceededCount) { this.exceededCount = exceededCount; return this; }

        public BudgetPerformanceDTO build() {
            return new BudgetPerformanceDTO(this);
        }
    }
}
