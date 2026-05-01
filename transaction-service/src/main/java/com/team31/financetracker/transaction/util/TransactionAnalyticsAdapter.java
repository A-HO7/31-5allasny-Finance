package com.team31.financetracker.transaction.util;

import com.team31.financetracker.transaction.dto.TransactionAnalyticsDTO;

import java.util.Map;

/**
 * Adapter Pattern (DP-7): Adapts a raw SQL Object projection (Map<String, Object>)
 * into a structured TransactionAnalyticsDTO.
 */
public class TransactionAnalyticsAdapter {

    public TransactionAnalyticsDTO adapt(Map<String, Object> raw) {
        if (raw == null) {
            return TransactionAnalyticsDTO.builder()
                    .totalTransactions(0)
                    .completedTransactions(0)
                    .voidedTransactions(0)
                    .totalIncome(0.0)
                    .totalExpenses(0.0)
                    .savingsRate(0.0)
                    .build();
        }

        int totalTransactions = toInt(raw.get("totalTransactions"));
        int completedTransactions = toInt(raw.get("completedTransactions"));
        int voidedTransactions = toInt(raw.get("voidedTransactions"));
        double totalIncome = toDouble(raw.get("totalIncome"));
        double totalExpenses = toDouble(raw.get("totalExpenses"));
        double savingsRate = (totalIncome > 0)
                ? ((totalIncome - totalExpenses) / totalIncome) * 100.0
                : 0.0;

        return TransactionAnalyticsDTO.builder()
                .totalTransactions(totalTransactions)
                .completedTransactions(completedTransactions)
                .voidedTransactions(voidedTransactions)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .savingsRate(savingsRate)
                .build();
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
