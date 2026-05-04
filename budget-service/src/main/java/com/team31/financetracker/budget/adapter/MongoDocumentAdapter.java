package com.team31.financetracker.budget.adapter;

import com.team31.financetracker.budget.model.BudgetEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter Pattern (DP-7) — adapts a MongoDB {@link BudgetEvent}
 * document into a generic Map for downstream consumers that don't
 * depend on the MongoDB entity directly.
 */
public class MongoDocumentAdapter {

    /**
     * Transform a MongoDB event document into a flat Map.
     *
     * @param source the MongoDB document entity (may be null)
     * @return a Map with standardised keys, or an empty Map if source is null
     */
    public Map<String, Object> adapt(BudgetEvent source) {
        if (source == null) {
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", source.getId());
        result.put("budgetId", source.getBudgetId());
        result.put("action", source.getAction());
        result.put("timestamp", source.getTimestamp());

        if (source.getDetails() != null) {
            result.put("details", source.getDetails());
        }

        return result;
    }
}
