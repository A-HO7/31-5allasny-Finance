package com.team31.financetracker.budget.factory;

import com.team31.financetracker.budget.model.BudgetEvent;
import com.team31.financetracker.budget.model.MongoEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory Pattern (DP-6) — creates {@link MongoEvent} instances.
 * Centralises event construction so callers do not depend on a concrete class.
 */
public class EventFactory {

    /**
     * Create a new event ready for persistence to MongoDB.
     *
     * @param budgetId the budget this event relates to (nullable)
     * @param action   UPPER_SNAKE_CASE action identifier
     * @param details  arbitrary key-value pairs with event-specific data
     * @return a fully populated {@link MongoEvent}
     */
    public static MongoEvent createEvent(Long budgetId, String action, Map<String, Object> details) {
        Map<String, Object> safeDetails = (details != null) ? new HashMap<>(details) : new HashMap<>();
        return new BudgetEvent(budgetId, action, LocalDateTime.now(), safeDetails);
    }
}
