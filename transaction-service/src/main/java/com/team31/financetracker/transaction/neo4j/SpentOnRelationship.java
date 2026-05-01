package com.team31.financetracker.transaction.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RelationshipProperties
public class SpentOnRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private CategoryNode category;

    @Property("transactionCount")
    private Integer transactionCount = 0;

    @Property("totalAmount")
    private Double totalAmount = 0.0;

    @Property("lastTransactionDate")
    private LocalDateTime lastTransactionDate;

    @Property("recordedTransactionIds")
    private List<Long> recordedTransactionIds = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    public SpentOnRelationship() {}

    public SpentOnRelationship(CategoryNode category) {
        this.category = category;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CategoryNode getCategory() { return category; }
    public void setCategory(CategoryNode category) { this.category = category; }

    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getLastTransactionDate() { return lastTransactionDate; }
    public void setLastTransactionDate(LocalDateTime lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }

    public List<Long> getRecordedTransactionIds() { return recordedTransactionIds; }
    public void setRecordedTransactionIds(List<Long> recordedTransactionIds) {
        this.recordedTransactionIds = recordedTransactionIds;
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    public boolean hasRecorded(Long transactionId) {
        return recordedTransactionIds != null && recordedTransactionIds.contains(transactionId);
    }

    /**
     * Increments counts and marks the transactionId as recorded.
     * Must be called inside a single Neo4j transaction (S3-F11 step d).
     */
    public void record(Long transactionId, Double amount, LocalDateTime when) {
        this.transactionCount = (this.transactionCount == null ? 0 : this.transactionCount) + 1;
        this.totalAmount = (this.totalAmount == null ? 0.0 : this.totalAmount) + amount;
        this.lastTransactionDate = when;
        if (this.recordedTransactionIds == null) this.recordedTransactionIds = new ArrayList<>();
        this.recordedTransactionIds.add(transactionId);
    }
}