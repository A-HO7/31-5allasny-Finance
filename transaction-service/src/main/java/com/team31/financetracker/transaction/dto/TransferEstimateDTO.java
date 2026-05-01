package com.team31.financetracker.transaction.dto;

public record TransferEstimateDTO(
        Double amount,
        Double transferFee,
        Double netTransfer,
        Double feePercentage) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double amount;
        private Double transferFee;
        private Double netTransfer;
        private Double feePercentage;

        public Builder amount(Double amount) {
            this.amount = amount;
            return this;
        }

        public Builder transferFee(Double transferFee) {
            this.transferFee = transferFee;
            return this;
        }

        public Builder netTransfer(Double netTransfer) {
            this.netTransfer = netTransfer;
            return this;
        }

        public Builder feePercentage(Double feePercentage) {
            this.feePercentage = feePercentage;
            return this;
        }

        public TransferEstimateDTO build() {
            return new TransferEstimateDTO(amount, transferFee, netTransfer, feePercentage);
        }
    }
}
