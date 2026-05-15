package com.team31.financetracker.contracts.events;
public record AccountStatusChangedEvent(Long accountId, Long userId, String oldStatus, String newStatus) {}
