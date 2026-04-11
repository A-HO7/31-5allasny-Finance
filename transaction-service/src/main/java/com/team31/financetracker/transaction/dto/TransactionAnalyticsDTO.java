package com.team31.financetracker.transaction.dto;

public record TransactionAnalyticsDTO(
        Integer totalTransactions,
        Integer completedTransactions,
        Integer voidedTransactions,
        Double totalIncome,
        Double totalExpenses,
        Double savingsRate) {
}
