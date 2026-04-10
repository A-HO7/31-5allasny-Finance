package com.team31.financetracker.reporting.dto;

import com.team31.financetracker.reporting.model.ReportType;

import java.time.LocalDate;

public class GenerateReportRequestDTO {
    private String name;
    private ReportType reportType;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    public GenerateReportRequestDTO() {
    }

    public GenerateReportRequestDTO(String name, ReportType reportType, LocalDate periodStart, LocalDate periodEnd) {
        this.name = name;
        this.reportType = reportType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
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
}
