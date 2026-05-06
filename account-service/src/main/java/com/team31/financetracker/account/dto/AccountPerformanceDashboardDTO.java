package com.team31.financetracker.account.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AccountPerformanceDashboardDTO {
    private Long accountId;
    private String name;
    private String type;
    private Double balance;
    private Long totalTransactions;
    private Double totalIncome;
    private Double totalExpenses;
    private Double netChange;
    private Long activeStatementsCount;
}
