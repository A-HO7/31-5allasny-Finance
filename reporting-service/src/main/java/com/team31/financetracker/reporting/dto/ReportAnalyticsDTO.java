package com.team31.financetracker.reporting.dto;

public class ReportAnalyticsDTO {

    private long totalGenerated;
    private long totalReports;
    private double averagePeriodDays;
    private long archivedCount;
    private long failedCount;

    public ReportAnalyticsDTO() {}

    private ReportAnalyticsDTO(Builder builder) {
        this.totalGenerated = builder.totalGenerated;
        this.totalReports = builder.totalReports;
        this.averagePeriodDays = builder.averagePeriodDays;
        this.archivedCount = builder.archivedCount;
        this.failedCount = builder.failedCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getTotalGenerated() { return totalGenerated; }
    public void setTotalGenerated(long totalGenerated) { this.totalGenerated = totalGenerated; }

    public long getTotalReports() { return totalReports; }
    public void setTotalReports(long totalReports) { this.totalReports = totalReports; }

    public double getAveragePeriodDays() { return averagePeriodDays; }
    public void setAveragePeriodDays(double averagePeriodDays) { this.averagePeriodDays = averagePeriodDays; }

    public long getArchivedCount() { return archivedCount; }
    public void setArchivedCount(long archivedCount) { this.archivedCount = archivedCount; }

    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }

    public static class Builder {
        private long totalGenerated;
        private long totalReports;
        private double averagePeriodDays;
        private long archivedCount;
        private long failedCount;

        public Builder totalGenerated(long totalGenerated) {
            this.totalGenerated = totalGenerated;
            return this;
        }

        public Builder totalReports(long totalReports) {
            this.totalReports = totalReports;
            return this;
        }

        public Builder averagePeriodDays(double averagePeriodDays) {
            this.averagePeriodDays = averagePeriodDays;
            return this;
        }

        public Builder archivedCount(long archivedCount) {
            this.archivedCount = archivedCount;
            return this;
        }

        public Builder failedCount(long failedCount) {
            this.failedCount = failedCount;
            return this;
        }

        public ReportAnalyticsDTO build() {
            return new ReportAnalyticsDTO(this);
        }
    }
}
