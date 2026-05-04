package com.team31.financetracker.budget.dto;

import java.io.Serializable;

/**
 * DTO for overspent budget responses.
 * Includes a static inner Builder (DP-4).
 */
public class OverspentBudgetDTO implements Serializable {

    private Long budgetId;
    private String userName;
    private String category;
    private Double budgetAmount;
    private Double spentAmount;
    private Double overspendPercentage;
    private Boolean warningSent;

    public OverspentBudgetDTO() {}

    private OverspentBudgetDTO(Builder builder) {
        this.budgetId = builder.budgetId;
        this.userName = builder.userName;
        this.category = builder.category;
        this.budgetAmount = builder.budgetAmount;
        this.spentAmount = builder.spentAmount;
        this.overspendPercentage = builder.overspendPercentage;
        this.warningSent = builder.warningSent;
    }

    // Getters & Setters

    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(Double budgetAmount) { this.budgetAmount = budgetAmount; }

    public Double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(Double spentAmount) { this.spentAmount = spentAmount; }

    public Double getOverspendPercentage() { return overspendPercentage; }
    public void setOverspendPercentage(Double overspendPercentage) { this.overspendPercentage = overspendPercentage; }

    public Boolean getWarningSent() { return warningSent; }
    public void setWarningSent(Boolean warningSent) { this.warningSent = warningSent; }

    // ──── Builder (DP-4) ────

    public static class Builder {
        private Long budgetId;
        private String userName;
        private String category;
        private Double budgetAmount;
        private Double spentAmount;
        private Double overspendPercentage;
        private Boolean warningSent;

        public Builder budgetId(Long budgetId) { this.budgetId = budgetId; return this; }
        public Builder userName(String userName) { this.userName = userName; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder budgetAmount(Double budgetAmount) { this.budgetAmount = budgetAmount; return this; }
        public Builder spentAmount(Double spentAmount) { this.spentAmount = spentAmount; return this; }
        public Builder overspendPercentage(Double overspendPercentage) { this.overspendPercentage = overspendPercentage; return this; }
        public Builder warningSent(Boolean warningSent) { this.warningSent = warningSent; return this; }

        public OverspentBudgetDTO build() {
            return new OverspentBudgetDTO(this);
        }
    }
}
