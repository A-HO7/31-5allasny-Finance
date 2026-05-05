package com.team31.financetracker.account.dto;

import com.team31.financetracker.account.model.AccountStatement;
import lombok.Builder;

import java.util.List;

@Builder
public record AccountStatementAlertDTO(
        Long accountId,
        String accountName,
        String accountStatus,
        List<AccountStatement> expiredStatements,
        Integer expiredCount
) {}