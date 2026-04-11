package com.team31.financetracker.reporting.service;

import com.team31.financetracker.reporting.dto.ReportTemplateUsageDTO;
import com.team31.financetracker.reporting.model.ReportTemplate;
import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.repository.ReportTemplateUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportTemplateUsageService {

    private final ReportTemplateUsageRepository repository;
    private final SavedReportService savedReportService;
    private final ReportTemplateService reportTemplateService;

    public ReportTemplateUsageService(ReportTemplateUsageRepository repository, 
                                     SavedReportService savedReportService, 
                                     ReportTemplateService reportTemplateService) {
        this.repository = repository;
        this.savedReportService = savedReportService;
        this.reportTemplateService = reportTemplateService;
    }

    @Transactional
    public ReportTemplateUsageDTO createReportTemplateUsage(ReportTemplateUsageDTO dto) {
        SavedReport report = savedReportService.getSavedReportById(dto.getReportId());
        ReportTemplate template = reportTemplateService.getReportTemplateById(dto.getTemplateId());

        ReportTemplateUsage usage = new ReportTemplateUsage();
        usage.setSavedReport(report);
        usage.setReportTemplate(template);
        usage.setPagesGenerated(dto.getPagesGenerated() != null ? dto.getPagesGenerated() : 0.0);
        usage.setAppliedAt(dto.getAppliedAt() != null ? dto.getAppliedAt() : LocalDateTime.now());

        ReportTemplateUsage saved = repository.save(usage);
        
        // Ensure bidirectional link is established for immediate reflection in parent
        report.getReportTemplateUsages().add(saved);
        
        return ReportTemplateUsageDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ReportTemplateUsageDTO> getAllReportTemplateUsages() {
        return repository.findAll().stream()
                .map(ReportTemplateUsageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReportTemplateUsageDTO getReportTemplateUsage(Long id) {
        ReportTemplateUsage usage = repository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("ReportTemplateUsage not found with id: " + id));
        return ReportTemplateUsageDTO.fromEntity(usage);
    }

    @Transactional
    public ReportTemplateUsageDTO updateReportTemplateUsage(Long id, ReportTemplateUsageDTO dto) {
        ReportTemplateUsage existing = repository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("ReportTemplateUsage not found with id: " + id));
        
        if (dto.getPagesGenerated() != null) existing.setPagesGenerated(dto.getPagesGenerated());
        if (dto.getAppliedAt() != null) existing.setAppliedAt(dto.getAppliedAt());
        
        // If IDs change, handle the association switch
        if (dto.getReportId() != null && !dto.getReportId().equals(existing.getSavedReport().getId())) {
            existing.getSavedReport().getReportTemplateUsages().remove(existing);
            SavedReport newReport = savedReportService.getSavedReportById(dto.getReportId());
            existing.setSavedReport(newReport);
            newReport.getReportTemplateUsages().add(existing);
        }
        
        if (dto.getTemplateId() != null && !dto.getTemplateId().equals(existing.getReportTemplate().getId())) {
            existing.setReportTemplate(reportTemplateService.getReportTemplateById(dto.getTemplateId()));
        }

        return ReportTemplateUsageDTO.fromEntity(repository.save(existing));
    }

    @Transactional
    public void deleteReportTemplateUsage(Long id) {
        ReportTemplateUsage usage = repository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("ReportTemplateUsage not found with id: " + id));
        
        // THE FIX: To truly delete a child in JPA with orphanRemoval=true,
        // we MUST remove it from the parent's collection. If we don't, 
        // Hibernate's 'Session Rescue' will re-persist it upon flush.
        SavedReport parentReport = usage.getSavedReport();
        if (parentReport != null) {
            parentReport.getReportTemplateUsages().remove(usage);
            // JPA orphanRemoval automatically deletes the usage row when it's removed from this list
        }
    }
}