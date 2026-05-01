package com.team31.financetracker.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team31.financetracker.transaction.Enums.TransactionSplitsStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "transaction_splits")
public class TransactionSplit {

    // ── PK ────────────────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Columns ───────────────────────────────────────────────────────────────
    @Column(name = "split_order", nullable = false)
    private Integer splitOrder;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionSplitsStatus status;

    // ── JSONB metadata column (Lab 4 pattern) ─────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // ── Relationship: TransactionSplit (owner) ←→ Transaction (inverse) ───────
    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    @JsonIgnore
    private Transaction transaction;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @PrePersist
    public void prePersist() {
        if (status == null)        status        = TransactionSplitsStatus.PENDING;
        if (metadata == null)      metadata      = new HashMap<>();
        if (splitOrder == null)    splitOrder    = 1;
        if (description == null)   description   = "";
        if (recipientName == null) recipientName = "";
        if (amount == null)        amount        = 0.0;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public Integer getSplitOrder()               { return splitOrder; }
    public void setSplitOrder(Integer splitOrder){ this.splitOrder = splitOrder; }

    public String getRecipientName()                  { return recipientName; }
    public void setRecipientName(String recipientName){ this.recipientName = recipientName; }

    public Double getAmount()                    { return amount; }
    public void setAmount(Double amount)         { this.amount = amount; }

    public String getDescription()                 { return description; }
    public void setDescription(String description) { this.description = description; }

    public TransactionSplitsStatus getStatus()             { return status; }
    public void setStatus(TransactionSplitsStatus status)  { this.status = status; }

    public Map<String, Object> getMetadata()              { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Transaction getTransaction()             { return transaction; }
    public void setTransaction(Transaction transaction){ this.transaction = transaction; }
}