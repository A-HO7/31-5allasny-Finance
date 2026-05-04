package com.team31.financetracker.reporting.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Response DTO for S5-F10 Get Financial Health Score.
 *
 * compositeScore is rounded to 1 decimal and clamped to [0, 100].
 * Component rates (savingsRate, budgetAdherenceRate, goalProgressRate,
 * accountLiquidityRate) are each clamped to [0, 100].
 *
 * Implements Serializable so Spring's Redis serializer can cache it.
 */
public class FinancialHealthScoreDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Double compositeScore;
    private Double savingsRate;
    private Double budgetAdherenceRate;
    private Double goalProgressRate;
    private Double accountLiquidityRate;
    private List<String> recommendations;

    public FinancialHealthScoreDTO() {}

    private FinancialHealthScoreDTO(Builder b) {
        this.userId               = b.userId;
        this.compositeScore       = b.compositeScore;
        this.savingsRate          = b.savingsRate;
        this.budgetAdherenceRate  = b.budgetAdherenceRate;
        this.goalProgressRate     = b.goalProgressRate;
        this.accountLiquidityRate = b.accountLiquidityRate;
        this.recommendations      = b.recommendations;
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long userId;
        private Double compositeScore;
        private Double savingsRate;
        private Double budgetAdherenceRate;
        private Double goalProgressRate;
        private Double accountLiquidityRate;
        private List<String> recommendations;

        public Builder userId(Long v)               { userId = v; return this; }
        public Builder compositeScore(Double v)      { compositeScore = v; return this; }
        public Builder savingsRate(Double v)         { savingsRate = v; return this; }
        public Builder budgetAdherenceRate(Double v) { budgetAdherenceRate = v; return this; }
        public Builder goalProgressRate(Double v)    { goalProgressRate = v; return this; }
        public Builder accountLiquidityRate(Double v){ accountLiquidityRate = v; return this; }
        public Builder recommendations(List<String> v){ recommendations = v; return this; }

        public FinancialHealthScoreDTO build() { return new FinancialHealthScoreDTO(this); }
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getUserId()               { return userId; }
    public void setUserId(Long v)         { userId = v; }

    public Double getCompositeScore()     { return compositeScore; }
    public void setCompositeScore(Double v){ compositeScore = v; }

    public Double getSavingsRate()        { return savingsRate; }
    public void setSavingsRate(Double v)  { savingsRate = v; }

    public Double getBudgetAdherenceRate()         { return budgetAdherenceRate; }
    public void setBudgetAdherenceRate(Double v)   { budgetAdherenceRate = v; }

    public Double getGoalProgressRate()            { return goalProgressRate; }
    public void setGoalProgressRate(Double v)      { goalProgressRate = v; }

    public Double getAccountLiquidityRate()        { return accountLiquidityRate; }
    public void setAccountLiquidityRate(Double v)  { accountLiquidityRate = v; }

    public List<String> getRecommendations()       { return recommendations; }
    public void setRecommendations(List<String> v) { recommendations = v; }
}
