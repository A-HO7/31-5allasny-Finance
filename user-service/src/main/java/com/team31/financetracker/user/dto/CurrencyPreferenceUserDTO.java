package com.team31.financetracker.user.dto;

import java.io.Serializable;

public class CurrencyPreferenceUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long userId;
    private String name;
    private Long completedTransactionCount;

    public CurrencyPreferenceUserDTO(Long userId, String name, Long completedTransactionCount) {
        this.userId = userId;
        this.name = name;
        this.completedTransactionCount = completedTransactionCount;
    }

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public Long getCompletedTransactionCount() { return completedTransactionCount; }

    // DP-4 Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long userId;
        private String name;
        private Long completedTransactionCount;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder completedTransactionCount(Long completedTransactionCount) {
            this.completedTransactionCount = completedTransactionCount;
            return this;
        }

        public CurrencyPreferenceUserDTO build() {
            return new CurrencyPreferenceUserDTO(userId, name, completedTransactionCount);
        }
    }
}
