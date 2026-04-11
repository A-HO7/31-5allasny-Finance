package com.team31.financetracker.reporting.dto;

public class ReportAnalyticsDTO {

    private long totalGenerated;
    private long totalReports;
    private double averagePeriodDays;
    private long archivedCount;
    private long failedCount;

    public ReportAnalyticsDTO() {}

    public ReportAnalyticsDTO(long totalGenerated, long totalReports, double averagePeriodDays,
                               long archivedCount, long failedCount) {
        this.totalGenerated = totalGenerated;
        this.totalReports = totalReports;
        this.averagePeriodDays = averagePeriodDays;
        this.archivedCount = archivedCount;
        this.failedCount = failedCount;
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
}
