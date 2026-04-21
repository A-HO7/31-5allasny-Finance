package com.team31.financetracker.budget.model;

public enum Category {
    SALARY,
    FOOD,
    TRANSPORT,
    RENT,
    SAVINGS,
    TRANSFER,
    BILLS,
    ENTERTAINMENT,
    SHOPPING,
    HEALTH,
    GROCERIES,
    @com.fasterxml.jackson.annotation.JsonEnumDefaultValue
    OTHER
}