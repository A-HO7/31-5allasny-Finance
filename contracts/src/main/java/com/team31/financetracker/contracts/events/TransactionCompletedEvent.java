package com.team31.financetracker.contracts.events;
import java.time.LocalDateTime;
public record TransactionCompletedEvent(Long transactionId, Long userId, Long accountId, String category, String type, Double amount, LocalDateTime completedAt) {}
