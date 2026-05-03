package com.team31.financetracker.reporting.adapter;

import com.team31.financetracker.reporting.dto.UserReportSummaryDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserReportSummaryAdapter {

    public UserReportSummaryDTO adapt(Long userId, List<Object[]> rows, long totalReports) {
        Map<String, Integer> typeBreakdown = new LinkedHashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                String type = row[0].toString();
                Integer count = ((Number) row[1]).intValue();
                typeBreakdown.put(type, count);
            }
        }
        long generatedCount = typeBreakdown.values().stream().mapToLong(Integer::longValue).sum();

        return UserReportSummaryDTO.builder()
                .userId(userId)
                .totalReports(totalReports)
                .generatedCount(generatedCount)
                .typeBreakdown(typeBreakdown)
                .build();
    }
}
