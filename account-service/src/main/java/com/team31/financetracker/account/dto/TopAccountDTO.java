package com.team31.financetracker.account.dto;

import lombok.Builder;

@Builder
public record TopAccountDTO(
        Long accountId,
        String name,
        Double balance,
        Long totalTransactions
) {}