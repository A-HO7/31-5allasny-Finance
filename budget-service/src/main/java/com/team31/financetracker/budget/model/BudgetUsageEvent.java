package com.team31.financetracker.budget.model;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("budget_usage_events")
public class BudgetUsageEvent {

    @PrimaryKey
    private BudgetUsageEventKey key;

    @Column("spent_amount")
    private Double spentAmount;

    @Column("remaining_amount")
    private Double remainingAmount;

    @Column("percent_used")
    private Double percentUsed;

    @Column("category")
    private String category;

    @Column("notes")
    private String notes;

    public BudgetUsageEvent() {}

    public BudgetUsageEvent(BudgetUsageEventKey key, Double spentAmount, Double remainingAmount,
                            Double percentUsed, String category, String notes) {
        this.key             = key;
        this.spentAmount     = spentAmount;
        this.remainingAmount = remainingAmount;
        this.percentUsed     = percentUsed;
        this.category        = category;
        this.notes           = notes;
    }

    public BudgetUsageEventKey getKey() { return key; }
    public void setKey(BudgetUsageEventKey key) { this.key = key; }

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
