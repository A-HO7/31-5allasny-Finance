package com.team31.financetracker.reporting.service;

import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import com.team31.financetracker.reporting.repository.ReportTemplateUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportTemplateUsageService {

    private final ReportTemplateUsageRepository repository;
    private final SavedReportService savedReportService;
    private final ReportTemplateService reportTemplateService;

    public ReportTemplateUsageService(ReportTemplateUsageRepository repository, SavedReportService savedReportService, ReportTemplateService reportTemplateService) {
        this.repository = repository;
        this.savedReportService = savedReportService;
        this.reportTemplateService = reportTemplateService;
    }

    public ReportTemplateUsage createReportTemplateUsage(ReportTemplateUsage usage) {
        if (usage.getSavedReport() != null && usage.getSavedReport().getId() != null) {
            usage.setSavedReport(savedReportService.getSavedReportById(usage.getSavedReport().getId()));
        }
        if (usage.getReportTemplate() != null && usage.getReportTemplate().getId() != null) {
            usage.setReportTemplate(reportTemplateService.getReportTemplateById(usage.getReportTemplate().getId()));
        }
        if (usage.getPagesGenerated() == null) {
            usage.setPagesGenerated(1.0);
        }
        return repository.saveAndFlush(usage);
    }

    public List<ReportTemplateUsage> getAllReportTemplateUsages() {
        return repository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ReportTemplateUsage getReportTemplateUsageById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("ReportTemplateUsage not found with id: " + id));
    }

    @Transactional
    public ReportTemplateUsage updateReportTemplateUsage(Long id, ReportTemplateUsage updatedUsage) {
        ReportTemplateUsage existing = getReportTemplateUsageById(id);
        if (updatedUsage.getPagesGenerated() != null) {
            existing.setPagesGenerated(updatedUsage.getPagesGenerated());
        }
        return repository.save(existing);
    }

    @Transactional
    public void deleteReportTemplateUsage(Long id) {
        ReportTemplateUsage existing = getReportTemplateUsageById(id);
        
        // Decouple from parent collections to avoid JPA CascadeType.ALL + orphanRemoval conflict
        if (existing.getSavedReport() != null && existing.getSavedReport().getReportTemplateUsages() != null) {
            existing.getSavedReport().getReportTemplateUsages().removeIf(u -> u.getId().equals(id));
        }
        if (existing.getReportTemplate() != null && existing.getReportTemplate().getReportTemplateUsages() != null) {
            existing.getReportTemplate().getReportTemplateUsages().removeIf(u -> u.getId().equals(id));
        }

        repository.delete(existing);
        repository.flush();
    }
 }