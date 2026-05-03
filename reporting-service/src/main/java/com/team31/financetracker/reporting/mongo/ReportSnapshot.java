package com.team31.financetracker.reporting.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Document(collection = "report_snapshots")
public class ReportSnapshot {


    @Id
    private String id;

    private Long reportId;
    private String status;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Map<String, Object> reportConfig;
    private List<TemplateUsageSnapshot> templateUsages;
    private LocalDateTime snapshotCreatedAt;

    public static class TemplateUsageSnapshot {

        private String templateCode;
        private String templateType;
        private Double pagesGenerated;
        private LocalDateTime appliedAt;
        public TemplateUsageSnapshot() {}
        public TemplateUsageSnapshot(String templateCode, String templateType,
                                     Double pagesGenerated, LocalDateTime appliedAt) {
            this.templateCode = templateCode;
            this.templateType = templateType;
            this.pagesGenerated = pagesGenerated;
            this.appliedAt = appliedAt;
        }
        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

        public String getTemplateType() { return templateType; }
        public void setTemplateType(String templateType) { this.templateType = templateType; }

        public Double getPagesGenerated() { return pagesGenerated; }
        public void setPagesGenerated(Double pagesGenerated) { this.pagesGenerated = pagesGenerated; }

        public LocalDateTime getAppliedAt() { return appliedAt; }
        public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    }

    public ReportSnapshot() {}
    public ReportSnapshot(Long reportId, String status,
                          LocalDate periodStart, LocalDate periodEnd,
                          Map<String, Object> reportConfig,
                          List<TemplateUsageSnapshot> templateUsages) {
        this.reportId = reportId;
        this.status = status;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.reportConfig = reportConfig;
        this.templateUsages = templateUsages;
        this.snapshotCreatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public Map<String, Object> getReportConfig() { return reportConfig; }
    public void setReportConfig(Map<String, Object> reportConfig) { this.reportConfig = reportConfig; }

    public List<TemplateUsageSnapshot> getTemplateUsages() { return templateUsages; }
    public void setTemplateUsages(List<TemplateUsageSnapshot> templateUsages) { this.templateUsages = templateUsages; }

    public LocalDateTime getSnapshotCreatedAt() { return snapshotCreatedAt; }
    public void setSnapshotCreatedAt(LocalDateTime snapshotCreatedAt) { this.snapshotCreatedAt = snapshotCreatedAt; }
}