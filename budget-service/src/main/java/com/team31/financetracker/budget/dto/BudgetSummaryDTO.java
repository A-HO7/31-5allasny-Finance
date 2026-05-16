package com.team31.financetracker.budget.dto;

public class BudgetSummaryDTO {

    private final double totalBudgeted;
    private final double totalSpent;
    private final double weightedAdherenceRate;

    public BudgetSummaryDTO(double totalBudgeted, double totalSpent, double weightedAdherenceRate) {
        this.totalBudgeted = totalBudgeted;
        this.totalSpent = totalSpent;
        this.weightedAdherenceRate = weightedAdherenceRate;
    }

    public double getTotalBudgeted() { return totalBudgeted; }
    public double getTotalSpent() { return totalSpent; }
    public double getWeightedAdherenceRate() { return weightedAdherenceRate; }
}
