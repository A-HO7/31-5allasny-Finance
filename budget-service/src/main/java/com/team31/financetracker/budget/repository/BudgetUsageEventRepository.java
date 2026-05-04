package com.team31.financetracker.budget.repository;

import com.team31.financetracker.budget.model.BudgetUsageEvent;
import com.team31.financetracker.budget.model.BudgetUsageEventKey;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.time.Instant;
import java.util.List;

public interface BudgetUsageEventRepository extends CassandraRepository<BudgetUsageEvent, BudgetUsageEventKey> {

    // Partition key is always required — all queries on this table go through budgetId
    List<BudgetUsageEvent> findByKeyBudgetId(Long budgetId);

    /**
     * S4-F12: Query usage events for a budget within an optional date range.
     * Uses the clustering key (timestamp) for range filtering.
     */
    List<BudgetUsageEvent> findByKeyBudgetIdAndKeyTimestampBetween(
            Long budgetId, Instant startTime, Instant endTime);
}
