package com.team31.financetracker.transaction.util;

import com.team31.financetracker.transaction.dto.TransactionAnalyticsDashboardDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionAnalyticsDashboardAdapter {

    public TransactionAnalyticsDashboardDTO adapt(
            Map<String, Object> raw,
            List<Object[]> categories,
            List<Object[]> statuses) {

        Integer totalTransactions = toInteger(raw.get("totalTransactions"));
        Double totalIncome = toDouble(raw.get("totalIncome"));
        Double totalExpenses = toDouble(raw.get("totalExpenses"));
        Double netFlow = totalIncome - totalExpenses;

        Integer completedTransactions = toInteger(raw.get("completedTransactions"));
        double completionRate = 0.0;
        if (totalTransactions != null && totalTransactions > 0) {
            completionRate = completedTransactions != null
                    ? (double) completedTransactions / totalTransactions
                    : 0.0;
        }

        Map<String, Integer> categoryCounts = convertCounts(categories);
        Map<String, Integer> statusCounts = convertCounts(statuses);

        return new TransactionAnalyticsDashboardDTO(
                totalTransactions,
                totalIncome,
                totalExpenses,
                netFlow,
                completionRate,
                categoryCounts,
                statusCounts);
    }

    private Map<String, Integer> convertCounts(List<Object[]> rows) {
        Map<String, Integer> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String key = row[0].toString();
            Integer count = toInteger(row[1]);
            result.put(key, count);
        }
        return result;
    }

    private Integer toInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer i) return i;
        if (value instanceof Long l) return l.intValue();
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(value.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double d) return d;
        if (value instanceof Float f) return f.doubleValue();
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (NumberFormatException e) { return 0.0; }
    }
}
