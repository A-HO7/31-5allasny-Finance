package com.team31.financetracker.budget.dto;

import com.team31.financetracker.budget.model.BudgetPeriod;
import com.team31.financetracker.budget.model.Category;

import java.time.LocalDate;
import java.util.Map;

public class BatchBudgetCreateItem {

    private Category category;
    private Double budgetAmount;
    private BudgetPeriod period;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Object> metadata;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Double getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(Double budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public BudgetPeriod getPeriod() {
        return period;
    }

    public void setPeriod(BudgetPeriod period) {
        this.period = period;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}