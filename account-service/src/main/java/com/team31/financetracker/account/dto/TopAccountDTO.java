package com.team31.financetracker.account.dto;

public record TopAccountDTO(
        Long accountId,
        String name,
        Double balance,
        Long totalTransactions
) {}