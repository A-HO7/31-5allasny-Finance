package com.team31.financetracker.user.model.nosql;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "report_audit_events")
public class ReportAuditEvent implements MongoEvent {
    @Id
    private String id;
    private String reportType;
    private Integer pagesGenerated;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public ReportAuditEvent(String reportType, Integer pagesGenerated, String action, Map<String, Object> details) {
        this.reportType = reportType;
        this.pagesGenerated = pagesGenerated;
        this.action = action;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    @Override public String getId() { return id; }
    @Override public LocalDateTime getTimestamp() { return timestamp; }
    @Override public String getAction() { return action; }
    @Override public Map<String, Object> getDetails() { return details; }

    public String getReportType() { return reportType; }
    public Integer getPagesGenerated() { return pagesGenerated; }
}
