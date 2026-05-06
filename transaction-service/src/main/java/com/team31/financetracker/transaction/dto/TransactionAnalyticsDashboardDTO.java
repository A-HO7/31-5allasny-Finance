package com.team31.financetracker.transaction.dto;

import java.util.Map;

public record TransactionAnalyticsDashboardDTO(
        Integer totalTransactions,
        Double totalIncome,
        Double totalExpenses,
        Double netFlow,
        Double completionRate,
        Map<String, Integer> transactionsByCategory,
        Map<String, Integer> transactionsByStatus) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer totalTransactions;
        private Double totalIncome;
        private Double totalExpenses;
        private Double netFlow;
        private Double completionRate;
        private Map<String, Integer> transactionsByCategory;
        private Map<String, Integer> transactionsByStatus;

        public Builder totalTransactions(Integer totalTransactions) {
            this.totalTransactions = totalTransactions;
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

        public Builder netFlow(Double netFlow) {
            this.netFlow = netFlow;
            return this;
        }

        public Builder completionRate(Double completionRate) {
            this.completionRate = completionRate;
            return this;
        }

        public Builder transactionsByCategory(Map<String, Integer> transactionsByCategory) {
            this.transactionsByCategory = transactionsByCategory;
            return this;
        }

        public Builder transactionsByStatus(Map<String, Integer> transactionsByStatus) {
            this.transactionsByStatus = transactionsByStatus;
            return this;
        }

        public TransactionAnalyticsDashboardDTO build() {
            return new TransactionAnalyticsDashboardDTO(
                    totalTransactions,
                    totalIncome,
                    totalExpenses,
                    netFlow,
                    completionRate,
                    transactionsByCategory,
                    transactionsByStatus);
        }
    }
}
