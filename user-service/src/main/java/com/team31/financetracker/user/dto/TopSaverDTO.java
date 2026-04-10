package com.team31.financetracker.user.dto;

public class TopSaverDTO {

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
}