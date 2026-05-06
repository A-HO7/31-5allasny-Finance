package com.team31.financetracker.account.dto;

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

    // Private constructor for builder
    private AccountPerformanceDashboardDTO() {}

    // Static builder method
    public static Builder builder() {
        return new Builder();
    }

    // Builder class
    public static class Builder {
        private final AccountPerformanceDashboardDTO dto = new AccountPerformanceDashboardDTO();

        public Builder accountId(Long accountId) {
            dto.accountId = accountId;
            return this;
        }

        public Builder name(String name) {
            dto.name = name;
            return this;
        }

        public Builder type(String type) {
            dto.type = type;
            return this;
        }

        public Builder balance(Double balance) {
            dto.balance = balance;
            return this;
        }

        public Builder totalTransactions(Long totalTransactions) {
            dto.totalTransactions = totalTransactions;
            return this;
        }

        public Builder totalIncome(Double totalIncome) {
            dto.totalIncome = totalIncome;
            return this;
        }

        public Builder totalExpenses(Double totalExpenses) {
            dto.totalExpenses = totalExpenses;
            return this;
        }

        public Builder netChange(Double netChange) {
            dto.netChange = netChange;
            return this;
        }

        public Builder activeStatementsCount(Long activeStatementsCount) {
            dto.activeStatementsCount = activeStatementsCount;
            return this;
        }

        public AccountPerformanceDashboardDTO build() {
            return dto;
        }
    }

    // Getters
    public Long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Double getBalance() {
        return balance;
    }

    public Long getTotalTransactions() {
        return totalTransactions;
    }

    public Double getTotalIncome() {
        return totalIncome;
    }

    public Double getTotalExpenses() {
        return totalExpenses;
    }

    public Double getNetChange() {
        return netChange;
    }

    public Long getActiveStatementsCount() {
        return activeStatementsCount;
    }
}