package com.team31.financetracker.budget.adapter;

import com.team31.financetracker.budget.dto.BudgetUsageDTO;
import com.team31.financetracker.budget.model.BudgetUsageEvent;

/**
 * Adapter Pattern (DP-7) — adapts a Cassandra {@link BudgetUsageEvent}
 * row into the API-facing {@link BudgetUsageDTO}.
 */
public class CassandraRowAdapter {

    /**
     * Transform a Cassandra entity into a DTO suitable for REST responses.
     *
     * @param source the Cassandra row entity (may be null)
     * @return the adapted DTO, or null if source is null
     */
    public BudgetUsageDTO adapt(BudgetUsageEvent source) {
        if (source == null) {
            return null;
        }

        BudgetUsageDTO dto = new BudgetUsageDTO();

        if (source.getKey() != null) {
            dto.setBudgetId(source.getKey().getBudgetId());
            dto.setTimestamp(source.getKey().getTimestamp());
        }

        dto.setSpentAmount(source.getSpentAmount());
        dto.setRemainingAmount(source.getRemainingAmount());
        dto.setPercentUsed(source.getPercentUsed());
        dto.setCategory(source.getCategory());
        dto.setNotes(source.getNotes());

        return dto;
    }
}
