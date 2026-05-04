package com.team31.financetracker.budget.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Marker interface for MongoDB event documents.
 * Part of the Factory Pattern (DP-6) — the EventFactory
 * returns instances of this type.
 */
public interface MongoEvent {

    String getId();

    Long getBudgetId();

    String getAction();

    LocalDateTime getTimestamp();

    Map<String, Object> getDetails();
}