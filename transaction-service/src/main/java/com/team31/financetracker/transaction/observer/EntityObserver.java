package com.team31.financetracker.transaction.observer;
/**
 * Observer Pattern (DP-2) — classical GoF Observer interface.
 *
 * <p>Each observer is notified whenever an observed subject (service) performs
 * a state-changing operation. The concrete implementation
 * {@link MongoEventLogger} writes the event to MongoDB.
 *
 * <p><b>Spring vs GoF note (Section 3.3):</b> Spring's
 * {@code @EventListener} mechanism must NOT be used for MongoDB event writes.
 * All MongoDB logging must flow through this classical Observer chain.
 *
 * @see MongoEventLogger
 */
public interface EntityObserver {

    /**
     * Called by the subject after a state change has been persisted.
     *
     * @param eventType  UPPER_SNAKE_CASE identifier of what happened
     *                   (e.g. {@code "APPROVED"}, {@code "SPLITS_ADDED"})
     * @param payload    the domain object (or DTO) that changed; the concrete
     *                   observer casts it as needed
     */
    void onEvent(String eventType, Object payload);
}
