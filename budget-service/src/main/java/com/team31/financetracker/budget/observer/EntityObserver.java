package com.team31.financetracker.budget.observer;

/**
 * Observer Pattern (DP-2) — classical GoF Observer interface.
 *
 * Each observer is notified whenever an observed subject service performs
 * a state-changing operation. The concrete implementation MongoEventLogger
 * writes the event to MongoDB.
 *
 * Spring vs GoF note: Spring's @EventListener mechanism must NOT be used for
 * MongoDB event writes. All MongoDB logging must flow through this classical
 * Observer chain.
 */
public interface EntityObserver {

    /**
     * Called by the subject after a state change has been persisted.
     *
     * @param eventType UPPER_SNAKE_CASE identifier, for example "USAGE_RECORDED"
     * @param payload   the domain object or Map containing event details
     */
    void onEvent(String eventType, Object payload);
}