package com.team31.financetracker.user.dto;

public class CurrencyPreferenceUserDTO {

    private Long userId;
    private String name;
    private Long completedTransactionCount;

    public CurrencyPreferenceUserDTO(Long userId, String name, Long completedTransactionCount) {
        this.userId = userId;
        this.name = name;
        this.completedTransactionCount = completedTransactionCount;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Long getCompletedTransactionCount() {
        return completedTransactionCount;
    }
}
