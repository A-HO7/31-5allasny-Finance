package com.team31.financetracker.account.dto;

import com.team31.financetracker.account.model.AccountStatus;

public class FreezeAccountRequest {
    private AccountStatus status;

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
