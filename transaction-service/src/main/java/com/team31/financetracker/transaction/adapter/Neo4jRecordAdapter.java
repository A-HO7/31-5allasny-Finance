package com.team31.financetracker.transaction.adapter;

import com.team31.financetracker.transaction.dto.CategoryRecommendationDTO;
import java.util.Map;

/**
 * Adapter Pattern (DP-7): Adapts a raw Neo4j result (Map<String, Object>)
 * into a structured CategoryRecommendationDTO.
 */
public class Neo4jRecordAdapter {

    public CategoryRecommendationDTO adapt(Map<String, Object> row) {
        if (row == null) return null;

        return CategoryRecommendationDTO.builder()
                .category((String) row.get("category"))
                .categoryType((String) row.get("categoryType"))
                .score(toInt(row.get("score")))
                .averageAmount(toDouble(row.get("averageAmount")))
                .build();
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
