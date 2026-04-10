package com.team31.financetracker.account.dto;

public class RateAccountRequest {
    private Long statementId;
    private Integer rating;

    public Long getStatementId() {
        return statementId;
    }

    public void setStatementId(Long statementId) {
        this.statementId = statementId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
