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

    public TemplateUsageDTO() {}

    private TemplateUsageDTO(Builder builder) {
        this.templateId = builder.templateId;
        this.code = builder.code;
        this.templateType = builder.templateType;
        this.maxPages = builder.maxPages;
        this.timesUsed = builder.timesUsed;
        this.totalPagesGenerated = builder.totalPagesGenerated;
        this.active = builder.active;
        this.expired = builder.expired;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getTemplateId() { return templateId; }
    public String getCode() { return code; }
    public String getTemplateType() { return templateType; }
    public Double getMaxPages() { return maxPages; }
    public Integer getTimesUsed() { return timesUsed; }
    public Double getTotalPagesGenerated() { return totalPagesGenerated; }
    public Boolean getActive() { return active; }
    public Boolean getExpired() { return expired; }

    public static class Builder {
        private Long templateId;
        private String code;
        private String templateType;
        private Double maxPages;
        private Integer timesUsed;
        private Double totalPagesGenerated;
        private Boolean active;
        private Boolean expired;

        public Builder templateId(Long templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder templateType(String templateType) {
            this.templateType = templateType;
            return this;
        }

        public Builder maxPages(Double maxPages) {
            this.maxPages = maxPages;
            return this;
        }

        public Builder timesUsed(Integer timesUsed) {
            this.timesUsed = timesUsed;
            return this;
        }

        public Builder totalPagesGenerated(Double totalPagesGenerated) {
            this.totalPagesGenerated = totalPagesGenerated;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder expired(Boolean expired) {
            this.expired = expired;
            return this;
        }

        public TemplateUsageDTO build() {
            return new TemplateUsageDTO(this);
        }
    }
}