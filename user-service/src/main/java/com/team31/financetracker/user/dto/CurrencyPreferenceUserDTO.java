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
