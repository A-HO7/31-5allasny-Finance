package com.team31.financetracker.budget.dto;

public class BudgetPerformanceDTO {

    private Long userId;
    private Integer totalBudgets;
    private Double totalBudgeted;
    private Double totalSpent;
    private Double averageUtilization;
    private Integer exceededCount;

    public BudgetPerformanceDTO() {}

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
}
