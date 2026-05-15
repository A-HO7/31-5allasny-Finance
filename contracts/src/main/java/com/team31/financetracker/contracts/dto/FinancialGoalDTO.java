package com.team31.financetracker.contracts.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

public class FinancialGoalDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String label;
    private Double targetAmount;
    private Double currentAmount;
    private LocalDate deadline;
    private Boolean isPrimary;
    private Map<String, Object> metadata;

    // All-args constructor (keeps existing UserService code working)
    public FinancialGoalDTO(String label, Double targetAmount, Double currentAmount,
                            LocalDate deadline, Boolean isPrimary, Map<String, Object> metadata) {
        this.label = label;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.deadline = deadline;
        this.isPrimary = isPrimary;
        this.metadata = metadata;
    }

    // Getters
    public String getLabel() { return label; }
    public Double getTargetAmount() { return targetAmount; }
    public Double getCurrentAmount() { return currentAmount; }
    public LocalDate getDeadline() { return deadline; }
    public Boolean getIsPrimary() { return isPrimary; }
    public Map<String, Object> getMetadata() { return metadata; }

    // DP-4 Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String label;
        private Double targetAmount;
        private Double currentAmount;
        private LocalDate deadline;
        private Boolean isPrimary;
        private Map<String, Object> metadata;

        public Builder label(String label) { this.label = label; return this; }
        public Builder targetAmount(Double targetAmount) { this.targetAmount = targetAmount; return this; }
        public Builder currentAmount(Double currentAmount) { this.currentAmount = currentAmount; return this; }
        public Builder deadline(LocalDate deadline) { this.deadline = deadline; return this; }
        public Builder isPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public FinancialGoalDTO build() {
            return new FinancialGoalDTO(label, targetAmount, currentAmount, deadline, isPrimary, metadata);
        }
    }
}
