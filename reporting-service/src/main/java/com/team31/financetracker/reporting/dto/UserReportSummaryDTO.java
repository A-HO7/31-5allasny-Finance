package com.team31.financetracker.reporting.dto;

import java.util.Map;

public class UserReportSummaryDTO {

    private Long userId;
    private Long totalReports;
    private Long generatedCount;
    private Map<String, Integer> typeBreakdown;

    public UserReportSummaryDTO(Long userId, Long totalReports, Long generatedCount, Map<String, Integer> typeBreakdown) {
        this.userId = userId;
        this.totalReports = totalReports;
        this.generatedCount = generatedCount;
        this.typeBreakdown = typeBreakdown;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTotalReports() { return totalReports; }
    public void setTotalReports(Long totalReports) { this.totalReports = totalReports; }

    public Long getGeneratedCount() { return generatedCount; }
    public void setGeneratedCount(Long generatedCount) { this.generatedCount = generatedCount; }

    public Map<String, Integer> getTypeBreakdown() { return typeBreakdown; }
    public void setTypeBreakdown(Map<String, Integer> typeBreakdown) { this.typeBreakdown = typeBreakdown; }
}
