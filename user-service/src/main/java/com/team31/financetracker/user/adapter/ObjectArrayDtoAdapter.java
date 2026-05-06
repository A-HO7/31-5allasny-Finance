package com.team31.financetracker.user.adapter;

import com.team31.financetracker.user.dto.TopSaverDTO;
import com.team31.financetracker.user.dto.UserTransactionSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class ObjectArrayDtoAdapter {

    public UserTransactionSummaryDTO adaptToUserTransactionSummary(Object[] row) {
        return UserTransactionSummaryDTO.builder()
                .userId(((Number) row[0]).longValue())
                .name((String) row[1])
                .totalTransactions(((Number) row[2]).longValue())
                .completedTransactions(((Number) row[3]).longValue())
                .voidedTransactions(((Number) row[4]).longValue())
                .totalIncome(((Number) row[5]).doubleValue())
                .totalExpenses(((Number) row[6]).doubleValue())
                .build();
    }

    public TopSaverDTO adaptToTopSaver(Object[] row) {
        return TopSaverDTO.builder()
                .userId(((Number) row[0]).longValue())
                .name((String) row[1])
                .netSavings(((Number) row[2]).doubleValue())
                .transactionCount(((Number) row[3]).longValue())
                .build();
    }
}
