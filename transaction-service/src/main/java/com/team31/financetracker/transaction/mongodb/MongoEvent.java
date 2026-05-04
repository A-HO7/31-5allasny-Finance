package com.team31.financetracker.transaction.mongodb;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Common interface implemented by all 5 MongoDB event document classes.
 * Required by EventFactory (DP-6) so it can return any concrete event typed
 * as MongoEvent.
 */
public interface MongoEvent {

    /** MongoDB ObjectId as a String. */
    String getId();

    /** When the event occurred. */
    LocalDateTime getTimestamp();

    /** Action identifier in UPPER_SNAKE_CASE (e.g. ANALYTICS_VIEWED). */
    String getAction();

    /** Additional event context / payload fields. */
    Map<String, Object> getDetails();
}
