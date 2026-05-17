package com.team31.financetracker.user.adapter;

import com.team31.financetracker.contracts.dto.TopSaverDTO;
import com.team31.financetracker.contracts.dto.UserTransactionSummaryDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ObjectArrayDtoAdapter {

    public UserTransactionSummaryDTO adaptToUserTransactionSummary(Object[] row) {
        if (row == null) return null;

        return UserTransactionSummaryDTO.builder()
                .userId(asLong(row[0]))
                .name(asString(row[1]))
                .totalTransactions(asLong(row[2]))
                .completedTransactions(asLong(row[3]))
                .voidedTransactions(asLong(row[4]))
                .totalIncome(asDouble(row[5]))
                .totalExpenses(asDouble(row[6]))
                .build();
    }

    public TopSaverDTO adaptToTopSaver(Object[] row) {
        if (row == null) return null;

        return TopSaverDTO.builder()
                .userId(asLong(row[0]))
                .name(asString(row[1]))
                .netSavings(asDouble(row[2]))
                .transactionCount(asLong(row[3]))
                .build();
    }

    // --- Helper Methods to prevent 500 NullPointerExceptions ---

    private Long asLong(Object obj) {
        if (rowValueIsNull(obj)) return 0L;
        if (obj instanceof Number num) return num.longValue();
        return 0L;
    }

    private Double asDouble(Object obj) {
        if (rowValueIsNull(obj)) return 0.0;
        if (obj instanceof Number num) return num.doubleValue();
        return 0.0;
    }

    private String asString(Object obj) {
        if (rowValueIsNull(obj)) return "";
        return String.valueOf(obj);
    }

    private boolean rowValueIsNull(Object obj) {
        return obj == null;
    }
}