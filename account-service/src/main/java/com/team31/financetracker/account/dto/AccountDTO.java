package com.team31.financetracker.account.dto;

import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountDTO {
    private Long id;
    private String name;
    private AccountType type;
    private String description; // Combined field from additive description key
    private String currency;
    private Double balance;
    private AccountStatus status;
    private Double rating;
}