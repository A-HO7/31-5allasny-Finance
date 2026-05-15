package com.team31.financetracker.contracts.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Mirrors the Transaction entity in transaction-service.
 * Fields: id, accountId, userId, type, amount, category, status, transactionDate.
 */
public class TransactionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long accountId;
    private Long userId;
    private String type;
    private String category;
    private Double amount;
    private LocalDateTime transactionDate;
    private String description;
    private String status;

    public TransactionDTO() {}

    public TransactionDTO(Long id, Long accountId, Long userId, String type, String category, 
                          Double amount, LocalDateTime transactionDate, String description, String status) {
        this.id = id;
        this.accountId = accountId;
        this.userId = userId;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long accountId;
        private Long userId;
        private String type;
        private String category;
        private Double amount;
        private LocalDateTime transactionDate;
        private String description;
        private String status;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder amount(Double amount) { this.amount = amount; return this; }
        public Builder transactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder status(String status) { this.status = status; return this; }

        public TransactionDTO build() {
            return new TransactionDTO(id, accountId, userId, type, category, amount, transactionDate, description, status);
        }
    }
}
