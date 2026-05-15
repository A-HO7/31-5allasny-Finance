package com.team31.financetracker.contracts.events;
public record ReportInitiatedEvent(Long reportId, Long transactionId, Long userId) {}
