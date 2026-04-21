package com.team31.financetracker.transaction.model;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team31.financetracker.transaction.Enums.TransactionSplitsStatus;

import java.util.HashMap;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transaction_splits")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer splitOrder;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionSplitsStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private java.util.Map<String, Object> metadata = new HashMap<>();

    // FIX: optional=true so that when a Transaction is created with embedded splits
    // in the request body, Hibernate does not immediately enforce the FK constraint
    // before the parent Transaction has been saved and its ID assigned.
    // The back-reference is set via Transaction.addTransactionSplit() / ensureSplitBackReferences()
    // before save(), so the FK will always be populated by the time the INSERT runs.
    @JsonIgnoreProperties("transactionSplits")
    @ManyToOne(optional = true)
    @JoinColumn(name = "transaction_id", nullable = true)
    private Transaction transaction;

    @com.fasterxml.jackson.annotation.JsonProperty("transactionId")
    public void setTransactionId(Long id) {
        if (id == null) return;   // ignore null — back-reference is set by the parent
        if (this.transaction == null) {
            this.transaction = new Transaction();
        }
        this.transaction.setId(id);
    }

    @com.fasterxml.jackson.annotation.JsonProperty("transaction_id")
    public void setTransactionIdSnakeCase(Long id) {
        setTransactionId(id);
    }

    @PrePersist
    public void prePersist() {
        if (status == null)
            status = TransactionSplitsStatus.PENDING;
        if (metadata == null)
            metadata = new HashMap<>();
        if (splitOrder == null)
            splitOrder = 1;
        if (description == null)
            description = "";
        if (recipientName == null)
            recipientName = "";
        if (amount == null)
            amount = 0.0;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSplitOrder() {
        return splitOrder;
    }

    public void setSplitOrder(Integer splitOrder) {
        this.splitOrder = splitOrder;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionSplitsStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionSplitsStatus status) {
        this.status = status;
    }

    public java.util.Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(java.util.Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
}