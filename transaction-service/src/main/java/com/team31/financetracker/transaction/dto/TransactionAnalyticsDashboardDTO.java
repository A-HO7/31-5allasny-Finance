package com.team31.financetracker.transaction.dto;

import java.util.Map;

public record TransactionAnalyticsDashboardDTO(
        Integer totalTransactions,
        Double totalIncome,
        Double totalExpenses,
        Double netFlow,
        Double completionRate,
        Map<String, Integer> transactionsByCategory,
        Map<String, Integer> transactionsByStatus) {
}
