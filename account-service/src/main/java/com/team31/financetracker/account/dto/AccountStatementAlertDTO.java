package com.team31.financetracker.account.dto;

import com.team31.financetracker.account.model.AccountStatement;

import java.util.List;

public class AccountStatementAlertDTO {
    private Long accountId;
    private String accountName;
    private String accountStatus;
    private List<AccountStatement> expiredStatements;
    private Integer expiredCount;

    public AccountStatementAlertDTO() {}

    public AccountStatementAlertDTO(Long accountId, String accountName, String accountStatus, List<AccountStatement> expiredStatements, Integer expiredCount) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountStatus = accountStatus;
        this.expiredStatements = expiredStatements;
        this.expiredCount = expiredCount;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public List<AccountStatement> getExpiredStatements() {
        return expiredStatements;
    }

    public void setExpiredStatements(List<AccountStatement> expiredStatements) {
        this.expiredStatements = expiredStatements;
    }

    public Integer getExpiredCount() {
        return expiredCount;
    }

    public void setExpiredCount(Integer expiredCount) {
        this.expiredCount = expiredCount;
    }

    public static class Builder {
        private Long accountId;
        private String accountName;
        private String accountStatus;
        private List<AccountStatement> expiredStatements;
        private Integer expiredCount;

        public Builder accountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder accountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        public Builder accountStatus(String accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public Builder expiredStatements(List<AccountStatement> expiredStatements) {
            this.expiredStatements = expiredStatements;
            return this;
        }

        public Builder expiredCount(Integer expiredCount) {
            this.expiredCount = expiredCount;
            return this;
        }

        public AccountStatementAlertDTO build() {
            return new AccountStatementAlertDTO(accountId, accountName, accountStatus, expiredStatements, expiredCount);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
