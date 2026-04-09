package com.team31.financetracker.budget.dto;

public class BatchBudgetCreateResponse {

    private int count;

    public BatchBudgetCreateResponse() {
    }

    public BatchBudgetCreateResponse(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}