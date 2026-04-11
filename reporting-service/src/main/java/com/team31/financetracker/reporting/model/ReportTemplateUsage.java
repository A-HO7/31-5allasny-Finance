package com.team31.financetracker.reporting.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @JsonDeserialize(using = com.team31.financetracker.reporting.util.FlexibleLocalDateTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @JsonIgnoreProperties("reportTemplateUsages")
    @ManyToOne(optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private SavedReport savedReport;

    @JsonIgnoreProperties("reportTemplateUsages")
    @ManyToOne(optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private ReportTemplate reportTemplate;

    @PrePersist
    protected void onCreate() {
        if (this.appliedAt == null) {
            this.appliedAt = LocalDateTime.now();
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
}
