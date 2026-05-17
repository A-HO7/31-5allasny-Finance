package com.team31.financetracker.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.team31.financetracker.transaction.util.FlexibleLocalDateTimeDeserializer;
import com.team31.financetracker.transaction.Enums.TransactionCategory;
import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.Enums.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "transactions")
public class Transaction {

    // ── PK ────────────────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Plain Long FK columns (cross-service references, not JPA-managed) ─────
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "to_account_id", nullable = true)
    private Long toAccountId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "approver_id", nullable = true)
    private Long approverId;

    // ── ENUM columns (stored as VARCHAR strings in PostgreSQL) ─────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'EGP'")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionCategory category;

    @Column(nullable = true, length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    // ── JSONB metadata column (Hibernate JSONB, Lab 4 pattern) ────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // ── Timestamps ────────────────────────────────────────────────────────────
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "completed_at", nullable = true)
    private LocalDateTime completedAt;

    // ── Relationship: Transaction (inverse) ←→ TransactionSplit (owner) ───────
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("splitOrder ASC")
    private List<TransactionSplit> transactionSplits = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @PrePersist
    public void prePersist() {
        if (transactionDate == null)
            transactionDate = LocalDateTime.now();
        if (status == null)
            status = TransactionStatus.PENDING;
        if (metadata == null)
            metadata = new HashMap<>();
        if (currency == null)
            currency = "EGP";
        if (type == null)
            type = TransactionType.EXPENSE;
        if (category == null)
            category = TransactionCategory.TRANSFER;
        if (accountId == null)
            accountId = 0L;
        if (userId == null)
            userId = 0L;
        if (amount == null)
            amount = 0.0;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<TransactionSplit> getTransactionSplits() {
        return transactionSplits;
    }

    public void setTransactionSplits(List<TransactionSplit> transactionSplits) {
        this.transactionSplits.clear();
        if (transactionSplits == null)
            return;
        for (TransactionSplit split : transactionSplits) {
            split.setTransaction(this);
            this.transactionSplits.add(split);
        }
    }

    /** Helper used by the service to add a split and keep back-reference intact. */
    public void addTransactionSplit(TransactionSplit split) {
        if (split == null)
            return;
        split.setTransaction(this);
        this.transactionSplits.add(split);
    }
}