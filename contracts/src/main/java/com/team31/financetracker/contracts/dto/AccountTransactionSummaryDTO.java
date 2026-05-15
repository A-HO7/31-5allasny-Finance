package com.team31.financetracker.contracts.dto;

import java.io.Serializable;

public class AccountTransactionSummaryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Double totalDeposits;
    private Double totalWithdrawals;
    private Double netChange;
    private Integer transactionCount;
    private String startDate;
    private String endDate;

    public AccountTransactionSummaryDTO() {}

    public AccountTransactionSummaryDTO(Double totalDeposits, Double totalWithdrawals, 
                                     Double netChange, Integer transactionCount,
                                     String startDate, String endDate) {
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.netChange = netChange;
        this.transactionCount = transactionCount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Double getTotalDeposits() { return totalDeposits; }
    public void setTotalDeposits(Double totalDeposits) { this.totalDeposits = totalDeposits; }

    public Double getTotalWithdrawals() { return totalWithdrawals; }
    public void setTotalWithdrawals(Double totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }

    public Double getNetChange() { return netChange; }
    public void setNetChange(Double netChange) { this.netChange = netChange; }

    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double totalDeposits;
        private Double totalWithdrawals;
        private Double netChange;
        private Integer transactionCount;
        private String startDate;
        private String endDate;

        public Builder totalDeposits(Double totalDeposits) { this.totalDeposits = totalDeposits; return this; }
        public Builder totalWithdrawals(Double totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; return this; }
        public Builder netChange(Double netChange) { this.netChange = netChange; return this; }
        public Builder transactionCount(Integer transactionCount) { this.transactionCount = transactionCount; return this; }
        public Builder startDate(String startDate) { this.startDate = startDate; return this; }
        public Builder endDate(String endDate) { this.endDate = endDate; return this; }

        public AccountTransactionSummaryDTO build() {
            return new AccountTransactionSummaryDTO(totalDeposits, totalWithdrawals, netChange, transactionCount, startDate, endDate);
        }
    }
}
