package com.team31.financetracker.budget.model;

public enum BudgetPeriod {
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    @com.fasterxml.jackson.annotation.JsonEnumDefaultValue
    YEARLY
}
