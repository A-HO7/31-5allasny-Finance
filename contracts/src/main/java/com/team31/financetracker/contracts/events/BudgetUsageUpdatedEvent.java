package com.team31.financetracker.contracts.events;
public record BudgetUsageUpdatedEvent(Long budgetId, Long userId, String category, Double spentAmount, String newStatus) {}
