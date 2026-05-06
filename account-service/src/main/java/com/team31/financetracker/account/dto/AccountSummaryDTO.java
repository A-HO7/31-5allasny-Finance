package com.team31.financetracker.account.dto;

import lombok.Builder;

@Builder
public record AccountSummaryDTO(
        Long accountId,
        String name,
        Double totalDeposits,
        Double totalWithdrawals,
        Double netChange,
        Long totalTransactions
) {}
