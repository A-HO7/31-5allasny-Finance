package com.team31.financetracker.contracts.events;
public record AccountStatsUpdatedEvent(Long accountId, int transactionDelta, Double balanceDelta) {}
