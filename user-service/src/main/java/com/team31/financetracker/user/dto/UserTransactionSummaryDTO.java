package com.team31.financetracker.user.dto;

public class UserTransactionSummaryDTO {

    private Long userId;
    private String name;
    private Long totalTransactions;
    private Long completedTransactions;
    private Long voidedTransactions;
    private Double totalIncome;
    private Double totalExpenses;

    public UserTransactionSummaryDTO(Long userId, String name,
                                     Long totalTransactions,
                                     Long completedTransactions,
                                     Long voidedTransactions,
                                     Double totalIncome,
                                     Double totalExpenses) {
        this.userId = userId;
        this.name = name;
        this.totalTransactions = totalTransactions;
        this.completedTransactions = completedTransactions;
        this.voidedTransactions = voidedTransactions;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Long getTotalTransactions() {
        return totalTransactions;
    }

    public Long getCompletedTransactions() {
        return completedTransactions;
    }

    public Long getVoidedTransactions() {
        return voidedTransactions;
    }

    public Double getTotalIncome() {
        return totalIncome;
    }

    public Double getTotalExpenses() {
        return totalExpenses;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String name;
        private Long totalTransactions;
        private Long completedTransactions;
        private Long voidedTransactions;
        private Double totalIncome;
        private Double totalExpenses;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder totalTransactions(Long totalTransactions) {
            this.totalTransactions = totalTransactions;
            return this;
        }

        public Builder completedTransactions(Long completedTransactions) {
            this.completedTransactions = completedTransactions;
            return this;
        }

        public Builder voidedTransactions(Long voidedTransactions) {
            this.voidedTransactions = voidedTransactions;
            return this;
        }

        public Builder totalIncome(Double totalIncome) {
            this.totalIncome = totalIncome;
            return this;
        }

        public Builder totalExpenses(Double totalExpenses) {
            this.totalExpenses = totalExpenses;
            return this;
        }

        public UserTransactionSummaryDTO build() {
            return new UserTransactionSummaryDTO(
                    userId,
                    name,
                    totalTransactions,
                    completedTransactions,
                    voidedTransactions,
                    totalIncome,
                    totalExpenses
            );
        }
    }
}
