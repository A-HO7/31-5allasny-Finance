package com.team31.financetracker.contracts.events;
public record TransactionApprovedEvent(Long transactionId, Long accountId, Long toAccountId, String type, Double amount, Long approverId) {}
