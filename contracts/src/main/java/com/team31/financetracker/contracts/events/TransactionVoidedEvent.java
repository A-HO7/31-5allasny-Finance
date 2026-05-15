package com.team31.financetracker.contracts.events;
public record TransactionVoidedEvent(Long transactionId, Long userId, Long accountId, Long toAccountId, String category, String type, Double amount, String previousStatus, String reason) {}
