package com.team31.financetracker.reporting.dto;

import java.util.Map;

public class UserReportSummaryDTO {

    private Long userId;
    private Long totalReports;
    private Long generatedCount;
    private Map<String, Integer> typeBreakdown;

    public UserReportSummaryDTO() {}

    private UserReportSummaryDTO(Builder builder) {
        this.userId = builder.userId;
        this.totalReports = builder.totalReports;
        this.generatedCount = builder.generatedCount;
        this.typeBreakdown = builder.typeBreakdown;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTotalReports() { return totalReports; }
    public void setTotalReports(Long totalReports) { this.totalReports = totalReports; }

    public Long getGeneratedCount() { return generatedCount; }
    public void setGeneratedCount(Long generatedCount) { this.generatedCount = generatedCount; }

    public Map<String, Integer> getTypeBreakdown() { return typeBreakdown; }
    public void setTypeBreakdown(Map<String, Integer> typeBreakdown) { this.typeBreakdown = typeBreakdown; }

    public static class Builder {
        private Long userId;
        private Long totalReports;
        private Long generatedCount;
        private Map<String, Integer> typeBreakdown;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder totalReports(Long totalReports) {
            this.totalReports = totalReports;
            return this;
        }

        public Builder generatedCount(Long generatedCount) {
            this.generatedCount = generatedCount;
            return this;
        }

        public Builder typeBreakdown(Map<String, Integer> typeBreakdown) {
            this.typeBreakdown = typeBreakdown;
            return this;
        }

        public UserReportSummaryDTO build() {
            return new UserReportSummaryDTO(this);
        }
    }
}
