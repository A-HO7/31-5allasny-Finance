package com.team31.financetracker.reporting.dto;

public class ReportAuditSummaryDTO {

    private String reportType;
    private long successCount;
    private long failureCount;
    private double successRate;
    private double totalPagesProduced;
    private long regenerationCount;

    public ReportAuditSummaryDTO() {}

    private ReportAuditSummaryDTO(Builder builder) {
        this.reportType = builder.reportType;
        this.successCount = builder.successCount;
        this.failureCount = builder.failureCount;
        this.successRate = builder.successRate;
        this.totalPagesProduced = builder.totalPagesProduced;
        this.regenerationCount = builder.regenerationCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public long getSuccessCount() { return successCount; }
    public void setSuccessCount(long successCount) { this.successCount = successCount; }

    public long getFailureCount() { return failureCount; }
    public void setFailureCount(long failureCount) { this.failureCount = failureCount; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public double getTotalPagesProduced() { return totalPagesProduced; }
    public void setTotalPagesProduced(double totalPagesProduced) { this.totalPagesProduced = totalPagesProduced; }

    public long getRegenerationCount() { return regenerationCount; }
    public void setRegenerationCount(long regenerationCount) { this.regenerationCount = regenerationCount; }

    public static class Builder {
        private String reportType;
        private long successCount;
        private long failureCount;
        private double successRate;
        private double totalPagesProduced;
        private long regenerationCount;

        public Builder reportType(String reportType) {
            this.reportType = reportType;
            return this;
        }

        public Builder successCount(long successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder failureCount(long failureCount) {
            this.failureCount = failureCount;
            return this;
        }

        public Builder successRate(double successRate) {
            this.successRate = successRate;
            return this;
        }

        public Builder totalPagesProduced(double totalPagesProduced) {
            this.totalPagesProduced = totalPagesProduced;
            return this;
        }

        public Builder regenerationCount(long regenerationCount) {
            this.regenerationCount = regenerationCount;
            return this;
        }

        public ReportAuditSummaryDTO build() {
            return new ReportAuditSummaryDTO(this);
        }
    }
}
