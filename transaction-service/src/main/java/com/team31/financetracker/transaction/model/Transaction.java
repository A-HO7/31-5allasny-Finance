package com.team31.financetracker.transaction.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.team31.financetracker.transaction.Enums.TransactionCategory;
import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.Enums.TransactionType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = true)
    private Long toAccountId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = true)
    private Long approverId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TransactionCategory category;

    @Column(nullable = true, length = 1024)
    private String description;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TransactionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = true)
    private LocalDateTime completedAt;

    @JsonManagedReference
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("splitOrder ASC")
    private List<TransactionSplit> transactionSplits = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

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
        if (transactionSplits == null) {
            return;
        }
        for (TransactionSplit split : transactionSplits) {
            addTransactionSplit(split);
        }
    }

    public void addTransactionSplit(TransactionSplit split) {
        if (split == null) {
            return;
        }
        split.setTransaction(this);
        this.transactionSplits.add(split);
    }
}
