package com.team31.financetracker.contracts.dto;

import java.util.Map;

public class AccountBalanceSummaryDTO {
    private long activeAccountCount;
    private double totalActiveBalance;
    private Map<String, Double> currencyBreakdown;

    public AccountBalanceSummaryDTO() {}

    public AccountBalanceSummaryDTO(long activeAccountCount, double totalActiveBalance, Map<String, Double> currencyBreakdown) {
        this.activeAccountCount = activeAccountCount;
        this.totalActiveBalance = totalActiveBalance;
        this.currencyBreakdown = currencyBreakdown;
    }

    public long getActiveAccountCount() { return activeAccountCount; }
    public void setActiveAccountCount(long activeAccountCount) { this.activeAccountCount = activeAccountCount; }

    public double getTotalActiveBalance() { return totalActiveBalance; }
    public void setTotalActiveBalance(double totalActiveBalance) { this.totalActiveBalance = totalActiveBalance; }

    public Map<String, Double> getCurrencyBreakdown() { return currencyBreakdown; }
    public void setCurrencyBreakdown(Map<String, Double> currencyBreakdown) { this.currencyBreakdown = currencyBreakdown; }
}
