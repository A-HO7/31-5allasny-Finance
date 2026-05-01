package com.team31.financetracker.reporting.mongo;

import com.team31.financetracker.reporting.observer.MongoEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document for reporting-service event audit trail.
 * Collection: report_audit_trail
 *
 * action values: GENERATED, FAILED, REGENERATED, REGENERATION_DENIED,
 *                ARCHIVED, TEMPLATE_APPLIED, ANALYTICS_VIEWED, REPORT_CREATED,
 *                REPORT_UPDATED, REPORT_DELETED
 *
 * reportType and pagesGenerated are required (non-null) on all lifecycle actions
 * (GENERATED, FAILED, REGENERATED, REGENERATION_DENIED, ARCHIVED, TEMPLATE_APPLIED).
 * They are null-permitted ONLY on ANALYTICS_VIEWED.
 */
@Document(collection = "report_audit_trail")
public class ReportAuditEvent implements MongoEvent {

    @Id
    private String id;

    @NonNull
    private Long reportId;

    @NonNull
    private String action;

    @NonNull
    private LocalDateTime timestamp;

    /**
     * Matches M1 SavedReport.reportType enum: MONTHLY_SUMMARY, CATEGORY_BREAKDOWN,
     * INCOME_VS_EXPENSE, NET_WORTH, CUSTOM.
     * Null only on ANALYTICS_VIEWED events.
     */
    private String reportType;

    /**
     * Sum of pagesGenerated across applied ReportTemplateUsage rows at time of event.
     * 0.0 when no templates applied yet.
     * Null only on ANALYTICS_VIEWED events.
     */
    private Double pagesGenerated;

    /**
     * Additional event context: strategy name, archive reason, failure reason, etc.
     */
    private Map<String, Object> details;

    public ReportAuditEvent() {}

    public ReportAuditEvent(Long reportId, String action, LocalDateTime timestamp,
                            String reportType, Double pagesGenerated, Map<String, Object> details) {
        this.reportId = reportId;
        this.action = action;
        this.timestamp = timestamp;
        this.reportType = reportType;
        this.pagesGenerated = pagesGenerated;
        this.details = details;
    }

    // ── MongoEvent interface ─────────────────────────────────────────────────

    @Override
    public String getId() { return id; }

    @Override
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String getAction() { return action; }

    @Override
    public Map<String, Object> getDetails() { return details; }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public void setId(String id) { this.id = id; }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public void setAction(String action) { this.action = action; }

    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public Double getPagesGenerated() { return pagesGenerated; }
    public void setPagesGenerated(Double pagesGenerated) { this.pagesGenerated = pagesGenerated; }

    public void setDetails(Map<String, Object> details) { this.details = details; }
}
