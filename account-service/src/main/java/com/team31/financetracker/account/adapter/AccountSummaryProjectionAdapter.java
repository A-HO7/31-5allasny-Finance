package com.team31.financetracker.account.adapter;

import com.team31.financetracker.account.dto.AccountSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class AccountSummaryProjectionAdapter {
    public AccountSummaryDTO adapt(Object[] row) {
        Long accountId = ((Number) row[0]).longValue();
        String name = (String) row[1];
        Double totalDeposits = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        Double totalWithdrawals = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        Long transactionCount = ((Number) row[4]).longValue();

        return AccountSummaryDTO.builder()
                .accountId(accountId)
                .name(name)
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .netChange(totalDeposits - totalWithdrawals)
                .totalTransactions(transactionCount)
                .build();
    }
}
