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
        if (usage.getSavedReport() == null || usage.getSavedReport().getId() == null || 
            usage.getReportTemplate() == null || usage.getReportTemplate().getId() == null || 
            usage.getPagesGenerated() == null) {
             throw new IllegalArgumentException("Missing required bounds (SavedReport, ReportTemplate references) or pages limit for the usage bridge creation.");
        }
        usage.setSavedReport(savedReportService.getSavedReportById(usage.getSavedReport().getId()));
        usage.setReportTemplate(reportTemplateService.getReportTemplateById(usage.getReportTemplate().getId()));
        return repository.save(usage);
    }

    public List<ReportTemplateUsage> getAllReportTemplateUsages() {
        return repository.findAll();
    }

    public ReportTemplateUsage getReportTemplateUsageById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("ReportTemplateUsage not found with id: " + id));
    }

    @Transactional
    public ReportTemplateUsage updateReportTemplateUsage(Long id, ReportTemplateUsage updatedUsage) {
        ReportTemplateUsage existing = getReportTemplateUsageById(id);
        
        if (updatedUsage.getPagesGenerated() == null) {
            throw new IllegalArgumentException("Pages generated value missing.");
        }

        existing.setPagesGenerated(updatedUsage.getPagesGenerated());
        return repository.save(existing);
    }

    @Transactional
    public void deleteReportTemplateUsage(Long id) {
        ReportTemplateUsage existing = getReportTemplateUsageById(id);
        repository.delete(existing);
    }
}
