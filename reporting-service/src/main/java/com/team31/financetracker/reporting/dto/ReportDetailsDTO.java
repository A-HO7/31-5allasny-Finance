package com.team31.financetracker.reporting.dto;

import com.team31.financetracker.reporting.model.ReportStatus;
import com.team31.financetracker.reporting.model.ReportType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportDetailsDTO {

    private Long reportId;
    private Long userId;
    private String name;
    private ReportType reportType;
    private ReportStatus status;
    private Map<String, Object> reportConfig;
    private List<AppliedTemplateDTO> appliedTemplates;
    private Double totalPages;
    private Integer templateCount;

    public static class AppliedTemplateDTO {
        private String templateCode;
        private String templateType;
        private Double pagesGenerated;
        private LocalDateTime appliedAt;

        public AppliedTemplateDTO(String templateCode, String templateType, Double pagesGenerated, LocalDateTime appliedAt) {
            this.templateCode = templateCode;
            this.templateType = templateType;
            this.pagesGenerated = pagesGenerated;
            this.appliedAt = appliedAt;
        }

        public String getTemplateCode() { return templateCode; }
        public String getTemplateType() { return templateType; }
        public Double getPagesGenerated() { return pagesGenerated; }
        public LocalDateTime getAppliedAt() { return appliedAt; }
    }

    public ReportDetailsDTO() {}

    private ReportDetailsDTO(Builder builder) {
        this.reportId = builder.reportId;
        this.userId = builder.userId;
        this.name = builder.name;
        this.reportType = builder.reportType;
        this.status = builder.status;
        this.reportConfig = builder.reportConfig;
        this.appliedTemplates = builder.appliedTemplates;
        this.totalPages = builder.totalPages;
        this.templateCount = builder.templateCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getReportId() { return reportId; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public ReportType getReportType() { return reportType; }
    public ReportStatus getStatus() { return status; }
    public Map<String, Object> getReportConfig() { return reportConfig; }
    public List<AppliedTemplateDTO> getAppliedTemplates() { return appliedTemplates; }
    public Double getTotalPages() { return totalPages; }
    public Integer getTemplateCount() { return templateCount; }

    public static class Builder {
        private Long reportId;
        private Long userId;
        private String name;
        private ReportType reportType;
        private ReportStatus status;
        private Map<String, Object> reportConfig;
        private List<AppliedTemplateDTO> appliedTemplates;
        private Double totalPages;
        private Integer templateCount;

        public Builder reportId(Long reportId) {
            this.reportId = reportId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder reportType(ReportType reportType) {
            this.reportType = reportType;
            return this;
        }

        public Builder status(ReportStatus status) {
            this.status = status;
            return this;
        }

        public Builder reportConfig(Map<String, Object> reportConfig) {
            this.reportConfig = reportConfig;
            return this;
        }

        public Builder appliedTemplates(List<AppliedTemplateDTO> appliedTemplates) {
            this.appliedTemplates = appliedTemplates;
            return this;
        }

        public Builder totalPages(Double totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder templateCount(Integer templateCount) {
            this.templateCount = templateCount;
            return this;
        }

        public ReportDetailsDTO build() {
            return new ReportDetailsDTO(this);
        }
    }
}