package com.team31.financetracker.transaction.mongodb;

/**
 * Discriminator enum used by {@link EventFactory} to decide which concrete
 * {@link MongoEvent} subtype to instantiate.
 *
 * Each of the 5 services binds to exactly one EventType at construction time:
 * transaction-service binds TRANSACTION.
 */
public enum EventType {
    AUTH,
    ACCOUNT,
    TRANSACTION,
    BUDGET,
    REPORT_AUDIT
}
