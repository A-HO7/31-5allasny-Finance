package com.team31.financetracker.transaction.dto;

public record TransferEstimateDTO(
        Double amount,
        Double transferFee,
        Double netTransfer,
        Double feePercentage) {
}
