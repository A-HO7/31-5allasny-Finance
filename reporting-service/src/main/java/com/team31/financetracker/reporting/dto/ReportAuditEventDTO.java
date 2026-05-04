package com.team31.financetracker.reporting.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ReportAuditEventDTO {
    private String id;
    private Long reportId;
    private String action;
    private LocalDateTime timestamp;
    private String reportType;
    private Double pagesGenerated;
    private Map<String, Object> details;

    public ReportAuditEventDTO() {}

    public ReportAuditEventDTO(String id, Long reportId, String action, LocalDateTime timestamp, 
                               String reportType, Double pagesGenerated, Map<String, Object> details) {
        this.id = id;
        this.reportId = reportId;
        this.action = action;
        this.timestamp = timestamp;
        this.reportType = reportType;
        this.pagesGenerated = pagesGenerated;
        this.details = details;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public Double getPagesGenerated() { return pagesGenerated; }
    public void setPagesGenerated(Double pagesGenerated) { this.pagesGenerated = pagesGenerated; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
