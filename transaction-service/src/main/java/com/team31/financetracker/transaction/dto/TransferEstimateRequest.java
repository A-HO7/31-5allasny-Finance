package com.team31.financetracker.transaction.dto;

public record TransferEstimateRequest(Long accountId, Long toAccountId, Double amount) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long accountId;
        private Long toAccountId;
        private Double amount;

        public Builder accountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder toAccountId(Long toAccountId) {
            this.toAccountId = toAccountId;
            return this;
        }

        public Builder amount(Double amount) {
            this.amount = amount;
            return this;
        }

        public TransferEstimateRequest build() {
            return new TransferEstimateRequest(accountId, toAccountId, amount);
        }
    }
}
