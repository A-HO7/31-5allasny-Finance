package com.team31.financetracker.contracts.dto;

import java.io.Serializable;

/**
 * Mirrors account-service's AccountDTO.
 * Fields: id, name, type (String), description, currency, balance, status (String), rating.
 * Enums are represented as Strings to avoid a cross-module enum dependency.
 */
public class AccountDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String type;
    private String description;
    private String currency;
    private Double balance;
    private String status;
    private Double rating;

    public AccountDTO() {}

    public AccountDTO(Long id, String name, String type, String description, String currency,
                      Double balance, String status, Double rating) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.rating = rating;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String type;
        private String description;
        private String currency;
        private Double balance;
        private String status;
        private Double rating;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder balance(Double balance) { this.balance = balance; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder rating(Double rating) { this.rating = rating; return this; }

        public AccountDTO build() {
            return new AccountDTO(id, name, type, description, currency, balance, status, rating);
        }
    }
}
