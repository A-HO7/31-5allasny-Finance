package com.team31.financetracker.contracts.events;
public record ReportFailedEvent(Long reportId, Long transactionId, Long userId, String reason) {}
