package com.team31.financetracker.reporting.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_template_usages")
public class ReportTemplateUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double pagesGenerated;

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnoreProperties("reportTemplateUsages")
    private SavedReport savedReport;

    @ManyToOne(optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonIgnoreProperties("reportTemplateUsages")
    private ReportTemplate reportTemplate;

    @PrePersist
    protected void onCreate() {
        if (this.appliedAt == null) {
            this.appliedAt = LocalDateTime.now();
        }
    }

    @PreRemove
    protected void onRemove() {
        if (this.reportTemplate != null) {
            int current = this.reportTemplate.getCurrentUses();
            this.reportTemplate.setCurrentUses(Math.max(0, current - 1));
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getPagesGenerated() {
        return pagesGenerated;
    }

    public void setPagesGenerated(Double pagesGenerated) {
        this.pagesGenerated = pagesGenerated;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public SavedReport getSavedReport() {
        return savedReport;
    }

    public void setSavedReport(SavedReport savedReport) {
        this.savedReport = savedReport;
    }

    public ReportTemplate getReportTemplate() {
        return reportTemplate;
    }

    public void setReportTemplate(ReportTemplate reportTemplate) {
        this.reportTemplate = reportTemplate;
    }

    @JsonProperty("reportId")
    public Long getReportId() {
        return savedReport != null ? savedReport.getId() : null;
    }

    @JsonProperty("templateId")
    public Long getTemplateId() {
        return reportTemplate != null ? reportTemplate.getId() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportTemplateUsage)) return false;
        ReportTemplateUsage that = (ReportTemplateUsage) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
