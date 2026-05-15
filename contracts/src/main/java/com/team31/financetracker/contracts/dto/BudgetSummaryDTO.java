package com.team31.financetracker.contracts.dto;

public class BudgetSummaryDTO {
    private double totalBudgeted;
    private double totalSpent;
    private double weightedAdherenceRate;

    public BudgetSummaryDTO() {}

    public BudgetSummaryDTO(double totalBudgeted, double totalSpent, double weightedAdherenceRate) {
        this.totalBudgeted = totalBudgeted;
        this.totalSpent = totalSpent;
        this.weightedAdherenceRate = weightedAdherenceRate;
    }

    public double getTotalBudgeted() { return totalBudgeted; }
    public void setTotalBudgeted(double totalBudgeted) { this.totalBudgeted = totalBudgeted; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public double getWeightedAdherenceRate() { return weightedAdherenceRate; }
    public void setWeightedAdherenceRate(double weightedAdherenceRate) { this.weightedAdherenceRate = weightedAdherenceRate; }
}
