package com.team31.financetracker.contracts.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Mirrors account-service's AccountDTO.
 * Fields: id, name, type (String), description, currency, balance, status (String), rating.
 * Enums are represented as Strings to avoid a cross-module enum dependency.
 */
public class AccountDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String name;
    private String type;
    private String currency;
    private Double balance;
    private String status;
    private Double rating;
    private Map<String, Object> accountDetails;

    public AccountDTO() {}

    public AccountDTO(
            Long id,
            Long userId,
            String name,
            String type,
            String currency,
            Double balance,
            String status,
            Double rating,
            Map<String, Object> accountDetails
    ) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.rating = rating;
        this.accountDetails = accountDetails;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Map<String, Object> getAccountDetails() {
        return accountDetails;
    }

    public void setAccountDetails(Map<String, Object> accountDetails) {
        this.accountDetails = accountDetails;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private String name;
        private String type;
        private String currency;
        private Double balance;
        private String status;
        private Double rating;
        private Map<String, Object> accountDetails;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder balance(Double balance) { this.balance = balance; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder rating(Double rating) { this.rating = rating; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder accountDetails(Map<String, Object> accountDetails) { this.accountDetails = accountDetails; return this; }

        public AccountDTO build() {
            return new AccountDTO(id, userId, name, type, currency, balance, status, rating, accountDetails);
        }
    }
}
