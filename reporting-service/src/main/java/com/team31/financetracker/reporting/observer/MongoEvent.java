package com.team31.financetracker.reporting.observer;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Common interface for all MongoDB event documents across services.
 * Allows EventFactory to return any concrete event typed as MongoEvent.
 * DP-6 (Factory) relies on this interface as the return type.
 */
public interface MongoEvent {

    /** MongoDB ObjectId as String */
    String getId();

    /** When the event occurred */
    LocalDateTime getTimestamp();

    /** Action identifier in UPPER_SNAKE_CASE (e.g. GENERATED, ARCHIVED) */
    String getAction();

    /** Additional event context — service-specific details */
    Map<String, Object> getDetails();
}
