package com.team31.financetracker.reporting.service;

import com.team31.financetracker.reporting.model.ReportTemplate;
import com.team31.financetracker.reporting.repository.ReportTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.team31.financetracker.reporting.dto.TemplateUsageDTO;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class ReportTemplateService {

    private final ReportTemplateRepository repository;

    public ReportTemplateService(ReportTemplateRepository repository) {
        this.repository = repository;
    }

    public ReportTemplate createReportTemplate(ReportTemplate template) {
        if (template.getCode() != null && repository.existsByCode(template.getCode())) {
            throw new IllegalArgumentException("Template code must be unique");
        }
        return repository.save(template);
    }

    public List<ReportTemplate> getAllReportTemplates() {
        return repository.findAll();
    }

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

        return repository.save(existing);
    }

    @Transactional
    public void deleteReportTemplate(Long id) {
        ReportTemplate existing = getReportTemplateById(id);
        repository.delete(existing);
    }

    public List<TemplateUsageDTO> getTopUsedTemplates(int limit) {
        List<Object[]> rows = repository.findTopUsedTemplates(limit);
        return rows.stream().map(row -> {
            Long templateId = ((Number) row[0]).longValue();
            String code = (String) row[1];
            String templateType = (String) row[2];
            Double maxPages = ((Number) row[3]).doubleValue();
            Integer timesUsed = ((Number) row[4]).intValue();
            Double totalPagesGenerated = ((Number) row[5]).doubleValue();
            Boolean active = (Boolean) row[6];
            LocalDateTime expiryDate = null;
            if (row[7] != null) {
                if (row[7] instanceof java.sql.Timestamp) {
                    expiryDate = ((java.sql.Timestamp) row[7]).toLocalDateTime();
                } else if (row[7] instanceof LocalDateTime) {
                    expiryDate = (LocalDateTime) row[7];
                } else {
                    expiryDate = LocalDateTime.parse(row[7].toString());
                }
            }
            Boolean expired = expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
            return new TemplateUsageDTO(templateId, code, templateType, maxPages,
                    timesUsed, totalPagesGenerated, active, expired);
        }).collect(Collectors.toList());
    }
}
