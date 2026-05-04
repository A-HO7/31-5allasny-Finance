package com.team31.financetracker.budget.factory;

import com.team31.financetracker.budget.model.BudgetEvent;
import com.team31.financetracker.budget.model.MongoEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory Pattern (DP-6) — creates MongoEvent instances.
 * Centralises event construction so callers do not depend on a concrete class.
 */
@Component
public class EventFactory {

    /**
     * Create a new event ready for persistence to MongoDB.
     *
     * @param budgetId the budget this event relates to (nullable)
     * @param action   UPPER_SNAKE_CASE action identifier
     * @param details  arbitrary key-value pairs with event-specific data
     * @return a fully populated MongoEvent
     */
    public MongoEvent createEvent(Long budgetId, String action, Map<String, Object> details) {
        Map<String, Object> safeDetails =
                details != null ? new HashMap<>(details) : new HashMap<>();

        return new BudgetEvent(budgetId, action, LocalDateTime.now(), safeDetails);
    }
}