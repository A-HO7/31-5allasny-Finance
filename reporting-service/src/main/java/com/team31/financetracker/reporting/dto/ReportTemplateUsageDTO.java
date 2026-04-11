package com.team31.financetracker.reporting.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.team31.financetracker.reporting.model.ReportTemplateUsage;

import java.time.LocalDateTime;

public class ReportTemplateUsageDTO {

    private Long id;

    @JsonAlias({"report_id", "reportId"})
    private Long reportId;

    @JsonAlias({"template_id", "templateId"})
    private Long templateId;

    @JsonAlias({"pages_generated", "pagesGenerated"})
    private Double pagesGenerated;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonAlias({"applied_at", "appliedAt"})
    private LocalDateTime appliedAt;

    public ReportTemplateUsageDTO() {}

    public ReportTemplateUsageDTO(Long id, Long reportId, Long templateId, Double pagesGenerated, LocalDateTime appliedAt) {
        this.id = id;
        this.reportId = reportId;
        this.templateId = templateId;
        this.pagesGenerated = pagesGenerated;
        this.appliedAt = appliedAt;
    }

    public static ReportTemplateUsageDTO fromEntity(ReportTemplateUsage entity) {
        if (entity == null) return null;
        return new ReportTemplateUsageDTO(
                entity.getId(),
                entity.getSavedReport() != null ? entity.getSavedReport().getId() : null,
                entity.getReportTemplate() != null ? entity.getReportTemplate().getId() : null,
                entity.getPagesGenerated(),
                entity.getAppliedAt()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public Double getPagesGenerated() { return pagesGenerated; }
    public void setPagesGenerated(Double pagesGenerated) { this.pagesGenerated = pagesGenerated; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
