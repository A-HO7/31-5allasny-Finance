package com.team31.financetracker.account.dto;

public record AccountSummaryDTO(
        Long accountId,
        String name,
        Double totalDeposits,
        Double totalWithdrawals,
        Double netChange,
        Long transactionCount
) {}
