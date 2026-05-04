package com.team31.financetracker.budget.model;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@PrimaryKeyClass
public class BudgetUsageEventKey implements Serializable {

    @PrimaryKeyColumn(name = "budget_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long budgetId;

    @PrimaryKeyColumn(name = "timestamp", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant timestamp;

    public BudgetUsageEventKey() {}

    public BudgetUsageEventKey(Long budgetId, Instant timestamp) {
        this.budgetId  = budgetId;
        this.timestamp = timestamp;
    }

    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BudgetUsageEventKey other)) return false;
        return Objects.equals(budgetId, other.budgetId) && Objects.equals(timestamp, other.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(budgetId, timestamp);
    }
}
