package com.team31.financetracker.transaction.model;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.team31.financetracker.transaction.Enums.TransactionSplitsStatus;

import java.util.HashMap;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transaction_splits")
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
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TransactionSplitsStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private java.util.Map<String, Object> metadata = new HashMap<>();

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @PrePersist
    public void prePersist() {
        if (status == null)
            status = TransactionSplitsStatus.PENDING;
        if (metadata == null)
            metadata = new HashMap<>();
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
