package com.team31.financetracker.budget.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for budget usage timeline responses (S4-F12).
 * Represents a single Cassandra usage snapshot adapted for the API consumer.
 */
public class BudgetUsageDTO implements Serializable {

    private Long budgetId;
    private Instant timestamp;
    private Double spentAmount;
    private Double remainingAmount;
    private Double percentUsed;
    private String category;
    private String notes;

    public BudgetUsageDTO() {}

    public BudgetUsageDTO(Long budgetId, Instant timestamp, Double spentAmount,
                          Double remainingAmount, Double percentUsed,
                          String category, String notes) {
        this.budgetId = budgetId;
        this.timestamp = timestamp;
        this.spentAmount = spentAmount;
        this.remainingAmount = remainingAmount;
        this.percentUsed = percentUsed;
        this.category = category;
        this.notes = notes;
    }

    // Getters & Setters

    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(Double spentAmount) { this.spentAmount = spentAmount; }

    public Double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Double remainingAmount) { this.remainingAmount = remainingAmount; }

    public Double getPercentUsed() { return percentUsed; }
    public void setPercentUsed(Double percentUsed) { this.percentUsed = percentUsed; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
