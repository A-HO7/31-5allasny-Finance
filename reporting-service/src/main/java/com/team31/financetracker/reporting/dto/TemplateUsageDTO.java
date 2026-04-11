package com.team31.financetracker.reporting.dto;

public class TemplateUsageDTO {

    private Long templateId;
    private String code;
    private String templateType;
    private Double maxPages;
    private Integer timesUsed;
    private Double totalPagesGenerated;
    private Boolean active;
    private Boolean expired;

    public TemplateUsageDTO(Long templateId, String code, String templateType,
                            Double maxPages, Integer timesUsed,
                            Double totalPagesGenerated, Boolean active, Boolean expired) {
        this.templateId = templateId;
        this.code = code;
        this.templateType = templateType;
        this.maxPages = maxPages;
        this.timesUsed = timesUsed;
        this.totalPagesGenerated = totalPagesGenerated;
        this.active = active;
        this.expired = expired;
    }

    public Long getTemplateId() { return templateId; }
    public String getCode() { return code; }
    public String getTemplateType() { return templateType; }
    public Double getMaxPages() { return maxPages; }
    public Integer getTimesUsed() { return timesUsed; }
    public Double getTotalPagesGenerated() { return totalPagesGenerated; }
    public Boolean getActive() { return active; }
    public Boolean getExpired() { return expired; }
}