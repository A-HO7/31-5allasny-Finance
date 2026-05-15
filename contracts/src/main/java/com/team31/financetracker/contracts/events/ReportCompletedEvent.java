package com.team31.financetracker.contracts.events;
public record ReportCompletedEvent(Long reportId, Long transactionId, Long userId, String reportType, Double pagesGenerated) {}
