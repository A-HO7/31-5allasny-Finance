package com.team31.financetracker.reporting.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.team31.financetracker.reporting.util.FlexibleLocalDateTimeDeserializer;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "saved_reports")
public class SavedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(nullable = false)
    private LocalDate periodStart;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> reportConfig = new java.util.LinkedHashMap<>();

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "savedReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportTemplateUsage> reportTemplateUsages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.status == null) this.status = ReportStatus.PENDING;
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.name == null) this.name = "Untitled Report";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public Map<String, Object> getReportConfig() {
        return reportConfig;
    }

    public void setReportConfig(Map<String, Object> reportConfig) {
        this.reportConfig = reportConfig;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ReportTemplateUsage> getReportTemplateUsages() {
        return reportTemplateUsages;
    }

    public void setReportTemplateUsages(List<ReportTemplateUsage> reportTemplateUsages) {
        this.reportTemplateUsages = reportTemplateUsages;
    }
}
