package com.team31.financetracker.reporting.adapter;

import com.team31.financetracker.reporting.dto.ReportAnalyticsDTO;
import org.springframework.stereotype.Component;

@Component
public class ReportAnalyticsAdapter implements ObjectArrayDtoAdapter<ReportAnalyticsDTO> {

    @Override
    public ReportAnalyticsDTO adapt(Object[] source) {
        if (source == null) {
            return ReportAnalyticsDTO.builder()
                    .totalGenerated(0)
                    .totalReports(0)
                    .averagePeriodDays(0.0)
                    .archivedCount(0)
                    .failedCount(0)
                    .build();
        }

        long totalReports = (source[0] != null) ? ((Number) source[0]).longValue() : 0;
        long totalGenerated = (source[1] != null) ? ((Number) source[1]).longValue() : 0;
        double averagePeriodDays = (source[2] != null) ? ((Number) source[2]).doubleValue() : 0.0;
        long archivedCount = (source[3] != null) ? ((Number) source[3]).longValue() : 0;
        long failedCount = (source[4] != null) ? ((Number) source[4]).longValue() : 0;

        return ReportAnalyticsDTO.builder()
                .totalReports(totalReports)
                .totalGenerated(totalGenerated)
                .averagePeriodDays(averagePeriodDays)
                .archivedCount(archivedCount)
                .failedCount(failedCount)
                .build();
    }
}
