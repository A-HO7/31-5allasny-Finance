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

    public ReportDetailsDTO(Long reportId, Long userId, String name, ReportType reportType,
                            ReportStatus status, Map<String, Object> reportConfig,
                            List<AppliedTemplateDTO> appliedTemplates, Double totalPages, Integer templateCount) {
        this.reportId = reportId;
        this.userId = userId;
        this.name = name;
        this.reportType = reportType;
        this.status = status;
        this.reportConfig = reportConfig;
        this.appliedTemplates = appliedTemplates;
        this.totalPages = totalPages;
        this.templateCount = templateCount;
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
}