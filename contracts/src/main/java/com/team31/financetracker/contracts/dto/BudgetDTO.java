package com.team31.financetracker.contracts.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Mirrors the Budget entity in budget-service.
 * Returns id, userId, category, amount, spentAmount, status, metadata.
 */
public class BudgetDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String category;
    private Double amount;
    private Double spentAmount;
    private String status;
    private Map<String, Object> metadata;

    public BudgetDTO() {}

    public BudgetDTO(Long id, Long userId, String category, Double amount, 
                     Double spentAmount, String status, Map<String, Object> metadata) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.amount = amount;
        this.spentAmount = spentAmount;
        this.status = status;
        this.metadata = metadata;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(Double spentAmount) { this.spentAmount = spentAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private String category;
        private Double amount;
        private Double spentAmount;
        private String status;
        private Map<String, Object> metadata;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder amount(Double amount) { this.amount = amount; return this; }
        public Builder spentAmount(Double spentAmount) { this.spentAmount = spentAmount; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public BudgetDTO build() {
            return new BudgetDTO(id, userId, category, amount, spentAmount, status, metadata);
        }
    }
}
