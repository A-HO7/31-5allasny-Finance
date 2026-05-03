package com.team31.financetracker.reporting.service;

import com.team31.financetracker.reporting.model.ReportTemplate;
import com.team31.financetracker.reporting.repository.ReportTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.stream.Collectors;

import com.team31.financetracker.reporting.dto.TemplateUsageDTO;
import com.team31.financetracker.reporting.adapter.TemplateUsageAdapter;

@Service
public class ReportTemplateService {

    private final ReportTemplateRepository repository;
    private final TemplateUsageAdapter templateUsageAdapter;
    private final com.team31.financetracker.reporting.util.RedisCacheEvictor redisCacheEvictor;

    public ReportTemplateService(ReportTemplateRepository repository, TemplateUsageAdapter templateUsageAdapter,com.team31.financetracker.reporting.util.RedisCacheEvictor redisCacheEvictor) {
        this.repository = repository;
        this.templateUsageAdapter = templateUsageAdapter;
        this.redisCacheEvictor = redisCacheEvictor;
    }

    public ReportTemplate createReportTemplate(ReportTemplate template) {
        if (template.getCode() != null && repository.existsByCode(template.getCode())) {
            throw new IllegalArgumentException("Template code must be unique");
        }
        ReportTemplate saved = repository.save(template);
        redisCacheEvictor.evictByPatterns(
                "reporting-service::report-template::" + saved.getId(),
                "reporting-service::S5-F9::*"
        );
        return saved;
    }

    public List<ReportTemplate> getAllReportTemplates() {
        return repository.findAll();
    }

    @Cacheable(value = "reporting-service::report-template", key = "#id")
    public ReportTemplate getReportTemplateById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("ReportTemplate not found with id: " + id));
    }

    @Transactional
    public ReportTemplate updateReportTemplate(Long id, ReportTemplate updatedTemplate) {
        ReportTemplate existing = getReportTemplateById(id);

        if (updatedTemplate.getCode() == null || updatedTemplate.getTemplateType() == null ||
            updatedTemplate.getMaxPages() == null || updatedTemplate.getMaxUses() == null ||
            updatedTemplate.getExpiryDate() == null) {
            throw new IllegalArgumentException("Missing required fields for ReportTemplate update process.");
        }

        existing.setCode(updatedTemplate.getCode());
        existing.setTemplateType(updatedTemplate.getTemplateType());
        existing.setMaxPages(updatedTemplate.getMaxPages());
        existing.setMaxUses(updatedTemplate.getMaxUses());
        existing.setExpiryDate(updatedTemplate.getExpiryDate());
        existing.setMetadata(updatedTemplate.getMetadata());

        if (updatedTemplate.getCurrentUses() != null) {
            existing.setCurrentUses(updatedTemplate.getCurrentUses());
        }
        if (updatedTemplate.getActive() != null) {
            existing.setActive(updatedTemplate.getActive());
        }

        ReportTemplate saved = repository.save(existing);
        redisCacheEvictor.evictByPatterns(
                "reporting-service::report-template::" + saved.getId(),
                "reporting-service::S5-F9::*"
        );
        return saved;
    }

    @Transactional
    public void deleteReportTemplate(Long id) {
        ReportTemplate existing = getReportTemplateById(id);
        repository.delete(existing);
        redisCacheEvictor.evictByPatterns(
                "reporting-service::report-template::" + id,
                "reporting-service::S5-F9::*"
        );
    }

    /**
     * S5-F9 — returns top-used templates as DTOs.
     * Uses native SQL returning Object[], mapped via TemplateUsageAdapter (Adapter Pattern).
     */
    @Cacheable(value = "reporting-service::S5-F9", key = "#limit")
    public List<TemplateUsageDTO> getTopUsedTemplates(int limit) {
        List<Object[]> rows = repository.findTopUsedTemplates(limit);
        return rows.stream()
                .map(templateUsageAdapter::adapt)
                .collect(Collectors.toList());
    }
}
