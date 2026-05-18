package com.team31.financetracker.account.dto;

public class AccountSummaryDTO {
    private Long accountId;
    private String name;
    private Double totalDeposits;
    private Double totalWithdrawals;
    private Double netChange;
    private Long totalTransactions;

    public AccountSummaryDTO() {}

    public AccountSummaryDTO(Long accountId, String name, Double totalDeposits, Double totalWithdrawals, Double netChange, Long totalTransactions) {
        this.accountId = accountId;
        this.name = name;
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.netChange = netChange;
        this.totalTransactions = totalTransactions;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(Double totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public Double getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public void setTotalWithdrawals(Double totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }

    public Double getNetChange() {
        return netChange;
    }

    public void setNetChange(Double netChange) {
        this.netChange = netChange;
    }

    public Long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(Long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public static class Builder {
        private Long accountId;
        private String name;
        private Double totalDeposits;
        private Double totalWithdrawals;
        private Double netChange;
        private Long totalTransactions;

        public Builder accountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder totalDeposits(Double totalDeposits) {
            this.totalDeposits = totalDeposits;
            return this;
        }

        public Builder totalWithdrawals(Double totalWithdrawals) {
            this.totalWithdrawals = totalWithdrawals;
            return this;
        }

        public Builder netChange(Double netChange) {
            this.netChange = netChange;
            return this;
        }

        public Builder totalTransactions(Long totalTransactions) {
            this.totalTransactions = totalTransactions;
            return this;
        }

        public AccountSummaryDTO build() {
            return new AccountSummaryDTO(accountId, name, totalDeposits, totalWithdrawals, netChange, totalTransactions);
        }
    }

    public static Builder builder (){
        return new Builder();
    }
}
