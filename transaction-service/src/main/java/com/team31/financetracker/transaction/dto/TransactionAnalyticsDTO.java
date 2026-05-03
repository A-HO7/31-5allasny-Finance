package com.team31.financetracker.transaction.dto;

public record TransactionAnalyticsDTO(
        Integer totalTransactions,
        Integer completedTransactions,
        Integer voidedTransactions,
        Double totalIncome,
        Double totalExpenses,
        Double savingsRate) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer totalTransactions;
        private Integer completedTransactions;
        private Integer voidedTransactions;
        private Double totalIncome;
        private Double totalExpenses;
        private Double savingsRate;

        public Builder totalTransactions(Integer totalTransactions) {
            this.totalTransactions = totalTransactions;
            return this;
        }

        public Builder completedTransactions(Integer completedTransactions) {
            this.completedTransactions = completedTransactions;
            return this;
        }

        public Builder voidedTransactions(Integer voidedTransactions) {
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

        public Builder savingsRate(Double savingsRate) {
            this.savingsRate = savingsRate;
            return this;
        }

        public TransactionAnalyticsDTO build() {
            return new TransactionAnalyticsDTO(
                    totalTransactions,
                    completedTransactions,
                    voidedTransactions,
                    totalIncome,
                    totalExpenses,
                    savingsRate);
        }
    }
}
