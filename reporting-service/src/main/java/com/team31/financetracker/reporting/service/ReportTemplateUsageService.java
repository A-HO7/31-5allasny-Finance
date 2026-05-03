package com.team31.financetracker.reporting.service;

import com.team31.financetracker.reporting.model.ReportTemplate;
import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.model.TemplateType;
import com.team31.financetracker.reporting.repository.ReportTemplateUsageRepository;
import com.team31.financetracker.reporting.repository.SavedReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.team31.financetracker.reporting.observer.EntityObserver;
import com.team31.financetracker.reporting.observer.MongoEventLogger;
import jakarta.annotation.PostConstruct;

@Service
public class ReportTemplateUsageService {

    private final ReportTemplateUsageRepository repository;
    private final SavedReportRepository savedReportRepository;
    private final ReportTemplateService reportTemplateService;
    private final MongoEventLogger mongoEventLogger;
    
    private final List<EntityObserver> observers = new CopyOnWriteArrayList<>();

    public ReportTemplateUsageService(ReportTemplateUsageRepository repository,
                                     SavedReportRepository savedReportRepository,
                                     ReportTemplateService reportTemplateService,
                                     MongoEventLogger mongoEventLogger) {
        this.repository = repository;
        this.savedReportRepository = savedReportRepository;
        this.reportTemplateService = reportTemplateService;
        this.mongoEventLogger = mongoEventLogger;
    }

    @PostConstruct
    public void init() {
        register(this.mongoEventLogger);
    }

    public void register(EntityObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregister(EntityObserver observer) {
        if (observer != null) {
            observers.remove(observer);
        }
    }

    protected void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    @Transactional
    public ReportTemplateUsage createReportTemplateUsage(ReportTemplateUsage usage) {
        if (usage.getSavedReport() == null || usage.getSavedReport().getId() == null ||
            usage.getReportTemplate() == null || usage.getReportTemplate().getId() == null) {
             throw new IllegalArgumentException("Missing required SavedReport or ReportTemplate references.");
        }
        
        SavedReport report = savedReportRepository.findById(usage.getSavedReport().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SavedReport not found"));
        ReportTemplate template = reportTemplateService.getReportTemplateById(usage.getReportTemplate().getId());
        
        if (!Boolean.TRUE.equals(template.getActive())) {
            throw new IllegalArgumentException("Template is not active.");
        }
        
        if (template.getExpiryDate() != null && template.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Template has expired.");
        }
        
        if (template.getCurrentUses() >= template.getMaxUses()) {
            throw new IllegalArgumentException("Template usage limit reached.");
        }

        // Business Logic: Calculate pagesGenerated if not provided
        if (usage.getPagesGenerated() == null) {
            long days = ChronoUnit.DAYS.between(report.getPeriodStart(), report.getPeriodEnd());
            double rate = (template.getTemplateType() == TemplateType.SUMMARY) ? 7.0 : 1.0;
            double calculated = days / rate;
            usage.setPagesGenerated(Math.min(calculated, template.getMaxPages()));
        }

        if (usage.getAppliedAt() == null) {
            usage.setAppliedAt(LocalDateTime.now());
        }

        usage.setSavedReport(report);
        usage.setReportTemplate(template);
        
        template.setCurrentUses(template.getCurrentUses() + 1);
        reportTemplateService.updateReportTemplate(template.getId(), template);

        ReportTemplateUsage saved = repository.save(usage);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("reportId", report.getId());
        payload.put("reportType", report.getReportType());
        payload.put("pagesGenerated", saved.getPagesGenerated());
        Map<String, Object> details = new HashMap<>();
        details.put("templateCode", template.getCode());
        payload.put("details", details);
        
        notifyObservers("TEMPLATE_APPLIED", payload);
        
        return saved;
    }

    public List<ReportTemplateUsage> getAllReportTemplateUsages() {
        return repository.findAll();
    }

    @Cacheable(value = "reporting-service::report-template-usage", key = "#id")
    public ReportTemplateUsage getReportTemplateUsageById(Long id) {
        return repository.findById(id).orElseThrow(() -> 
            new ResponseStatusException(HttpStatus.NOT_FOUND, "ReportTemplateUsage not found with id: " + id));
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
        
        // Manual unlinking from collections to avoid "zombies" in the session cache
        if (existing.getSavedReport() != null) {
            existing.getSavedReport().getReportTemplateUsages().remove(existing);
        }
        if (existing.getReportTemplate() != null) {
            existing.getReportTemplate().getReportTemplateUsages().remove(existing);
        }

        repository.delete(existing);
    }
}