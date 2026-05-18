package com.team31.financetracker.account.dto;

public class TopAccountDTO {
    private Long accountId;
    private String name;
    private Double balance;
    private Long totalTransactions;

    public TopAccountDTO() {}

    public TopAccountDTO(Long accountId, String name, Double balance, Long totalTransactions) {
        this.accountId = accountId;
        this.name = name;
        this.balance = balance;
        this.totalTransactions = totalTransactions;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public Double getBalance() {
        return balance;
    }

    public Long getTotalTransactions() {
        return totalTransactions;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public void setTotalTransactions(Long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public static class Builder {
        private Long accountId;
        private String name;
        private Double balance;
        private Long totalTransactions;

        public Builder accountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder balance(Double balance) {
            this.balance = balance;
            return this;
        }

        public Builder totalTransactions(Long totalTransactions) {
            this.totalTransactions = totalTransactions;
            return this;
        }

        public TopAccountDTO build() {
            return new TopAccountDTO(accountId, name, balance, totalTransactions);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
