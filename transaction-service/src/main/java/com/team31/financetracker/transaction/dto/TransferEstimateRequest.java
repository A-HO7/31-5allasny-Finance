package com.team31.financetracker.transaction.dto;

public record TransferEstimateRequest(Long accountId, Long toAccountId, Double amount) {
}
