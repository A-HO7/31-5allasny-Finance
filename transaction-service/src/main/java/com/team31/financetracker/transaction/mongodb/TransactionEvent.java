package com.team31.financetracker.transaction.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document stored in the {@code transaction_events} collection.
 * Implements {@link MongoEvent} so the {@link EventFactory} can return it
 * through the common interface.
 *
 * <p>Primary action values: ANALYTICS_VIEWED, PATTERN_RECORDED.
 * Non-exhaustive — extend with domain-appropriate UPPER_SNAKE_CASE values
 * for M1 Observer retrofits (e.g. APPROVED, COMPLETED, VOIDED, SPLITS_ADDED,
 * TRANSACTION_CREATED, TRANSACTION_DELETED).
 */
@Document(collection = "transaction_events")
public class TransactionEvent implements MongoEvent {

    @Id
    private String id;

    private Long transactionId;   // References Transaction in PG (not null)
    private String action;        // UPPER_SNAKE_CASE action identifier
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    // ── Constructors ──────────────────────────────────────────────────────────

    public TransactionEvent() {}

    public TransactionEvent(Long transactionId,
                            String action,
                            LocalDateTime timestamp,
                            Map<String, Object> details) {
        this.transactionId = transactionId;
        this.action        = action;
        this.timestamp     = timestamp;
        this.details       = details;
    }

    // ── MongoEvent interface ──────────────────────────────────────────────────

    @Override
    public String getId() { return id; }

    @Override
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String getAction() { return action; }

    @Override
    public Map<String, Object> getDetails() { return details; }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public void setId(String id) { this.id = id; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public void setAction(String action) { this.action = action; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
