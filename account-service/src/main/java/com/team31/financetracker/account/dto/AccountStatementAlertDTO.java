package com.team31.financetracker.account.dto;

import com.team31.financetracker.account.model.AccountStatement;
import java.util.List;

public record AccountStatementAlertDTO(
        Long accountId,
        String accountName,
        String accountStatus,
        List<AccountStatement> expiredStatements,
        Integer expiredCount
) {}