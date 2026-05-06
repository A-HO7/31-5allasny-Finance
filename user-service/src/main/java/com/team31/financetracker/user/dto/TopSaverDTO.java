package com.team31.financetracker.user.dto;

import java.io.Serializable;

public class TopSaverDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long userId;
    private String name;
    private Double netSavings;
    private Long transactionCount;

    public TopSaverDTO(Long userId, String name, Double netSavings, Long transactionCount) {
        this.userId = userId;
        this.name = name;
        this.netSavings = netSavings;
        this.transactionCount = transactionCount;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Double getNetSavings() {
        return netSavings;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String name;
        private Double netSavings;
        private Long transactionCount;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder netSavings(Double netSavings) {
            this.netSavings = netSavings;
            return this;
        }

        public Builder transactionCount(Long transactionCount) {
            this.transactionCount = transactionCount;
            return this;
        }

        public TopSaverDTO build() {
            return new TopSaverDTO(userId, name, netSavings, transactionCount);
        }
    }
}