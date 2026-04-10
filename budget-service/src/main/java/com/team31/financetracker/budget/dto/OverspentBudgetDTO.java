package com.team31.financetracker.budget.dto;

public class OverspentBudgetDTO {

    private Long budgetId;
    private String userName;
    private String category;
    private Double budgetAmount;
    private Double spentAmount;
    private Double overspendPercentage;
    private Boolean warningSent;

    public OverspentBudgetDTO() {}

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
}
