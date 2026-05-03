package com.team31.financetracker.reporting.adapter;

import com.team31.financetracker.reporting.dto.ReportAnalyticsDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportAnalyticsAdapter {

    public ReportAnalyticsDTO adapt(List<Object[]> results) {
        if (results == null || results.isEmpty() || results.get(0) == null) {
            return ReportAnalyticsDTO.builder()
                    .totalGenerated(0)
                    .totalReports(0)
                    .averagePeriodDays(0.0)
                    .archivedCount(0)
                    .failedCount(0)
                    .build();
        }

        Object[] row = results.get(0);
        long totalReports = (row[0] != null) ? ((Number) row[0]).longValue() : 0;
        long totalGenerated = (row[1] != null) ? ((Number) row[1]).longValue() : 0;
        double averagePeriodDays = (row[2] != null) ? ((Number) row[2]).doubleValue() : 0.0;
        long archivedCount = (row[3] != null) ? ((Number) row[3]).longValue() : 0;
        long failedCount = (row[4] != null) ? ((Number) row[4]).longValue() : 0;

        return ReportAnalyticsDTO.builder()
                .totalGenerated(totalGenerated)
                .totalReports(totalReports)
                .averagePeriodDays(averagePeriodDays)
                .archivedCount(archivedCount)
                .failedCount(failedCount)
                .build();
    }
}
