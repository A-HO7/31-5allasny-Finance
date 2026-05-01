package com.team31.financetracker.reporting.observer;

/**
 * Classical GoF Observer interface (DP-2).
 * Subjects (services) maintain a list of EntityObserver instances and call
 * notifyObservers(eventType, payload) on state changes.
 *
 * Important: No @EventListener annotation may write to MongoDB.
 * All MongoDB event writes must flow through this classical Observer chain.
 */
public interface EntityObserver {

    /**
     * Called by the subject when a state change occurs.
     *
     * @param eventType the action string in UPPER_SNAKE_CASE (e.g. "GENERATED", "ARCHIVED")
     * @param payload   the domain object or map carrying event details
     */
    void onEvent(String eventType, Object payload);
}
