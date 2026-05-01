package com.team31.financetracker.reporting.factory;

/**
 * Enum of supported MongoDB event types across all 5 services.
 * EventFactory dispatches on this type to return the correct MongoEvent subtype.
 *
 * Each service's MongoEventLogger is bound to one EventType at construction:
 *   - user-service        → AUTH
 *   - account-service     → ACCOUNT
 *   - transaction-service → TRANSACTION
 *   - budget-service      → BUDGET
 *   - reporting-service   → REPORT_AUDIT
 */
public enum EventType {
    AUTH,
    ACCOUNT,
    TRANSACTION,
    BUDGET,
    REPORT_AUDIT
}
