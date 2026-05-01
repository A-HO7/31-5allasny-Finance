package com.team31.financetracker.reporting.service;

import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.repository.SavedReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.team31.financetracker.reporting.dto.UserReportSummaryDTO;
import com.team31.financetracker.reporting.dto.GenerateReportRequestDTO;
import com.team31.financetracker.reporting.dto.ReportAnalyticsDTO;

import com.team31.financetracker.reporting.model.ReportType;
import com.team31.financetracker.reporting.model.ReportStatus;
import com.team31.financetracker.reporting.model.ReportTemplate;
import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import com.team31.financetracker.reporting.model.TemplateType;
import com.team31.financetracker.reporting.repository.ReportTemplateRepository;
import com.team31.financetracker.reporting.repository.ReportTemplateUsageRepository;
import java.time.temporal.ChronoUnit;

import com.team31.financetracker.reporting.dto.ReportDetailsDTO;
import java.util.stream.Collectors;
import com.team31.financetracker.reporting.observer.EntityObserver;
import com.team31.financetracker.reporting.observer.MongoEventLogger;
import java.util.concurrent.CopyOnWriteArrayList;
import jakarta.annotation.PostConstruct;

@Service
public class SavedReportService {

    private final SavedReportRepository repository;
    private final ReportTemplateRepository templateRepository;
    private final ReportTemplateUsageRepository usageRepository;
    private final ReportTemplateUsageService reportTemplateUsageService;
    private final MongoEventLogger mongoEventLogger;
    private final com.team31.financetracker.reporting.adapter.UserReportSummaryAdapter userReportSummaryAdapter;
    private final com.team31.financetracker.reporting.adapter.ReportAnalyticsAdapter reportAnalyticsAdapter;
    
    private final List<EntityObserver> observers = new CopyOnWriteArrayList<>();

    public SavedReportService(SavedReportRepository repository, 
                              ReportTemplateRepository templateRepository, 
                              ReportTemplateUsageRepository usageRepository,
                              ReportTemplateUsageService reportTemplateUsageService,
                              MongoEventLogger mongoEventLogger,
                              com.team31.financetracker.reporting.adapter.UserReportSummaryAdapter userReportSummaryAdapter,
                              com.team31.financetracker.reporting.adapter.ReportAnalyticsAdapter reportAnalyticsAdapter) {
        this.repository = repository;
        this.templateRepository = templateRepository;
        this.usageRepository = usageRepository;
        this.reportTemplateUsageService = reportTemplateUsageService;
        this.mongoEventLogger = mongoEventLogger;
        this.userReportSummaryAdapter = userReportSummaryAdapter;
        this.reportAnalyticsAdapter = reportAnalyticsAdapter;
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

    public SavedReport createSavedReport(SavedReport savedReport) {
        SavedReport saved = repository.save(savedReport);
        notifyReportEvent("REPORT_CREATED", saved, null);
        return saved;
    }

    public List<SavedReport> getAllSavedReports() {
        return repository.findAll();
    }

    public List<SavedReport> searchReports(ReportType reportType, ReportStatus status,
                                           LocalDate startDate, LocalDate endDate) {
        boolean hasType   = reportType != null;
        boolean hasStatus = status != null;
        boolean hasStart  = startDate != null;
        boolean hasEnd    = endDate != null;

        return repository.searchReports(hasType, reportType, hasStatus, status, hasStart, startDate, hasEnd, endDate);
    }

    public SavedReport getSavedReportById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("SavedReport not found with id: " + id));
    }

    @Transactional
    public SavedReport updateSavedReport(Long id, SavedReport updatedReport) {
        SavedReport existing = getSavedReportById(id);
        
        if (updatedReport.getUserId() != null) existing.setUserId(updatedReport.getUserId());
        if (updatedReport.getName() != null) existing.setName(updatedReport.getName());
        if (updatedReport.getReportType() != null) existing.setReportType(updatedReport.getReportType());
        if (updatedReport.getPeriodStart() != null) existing.setPeriodStart(updatedReport.getPeriodStart());
        if (updatedReport.getPeriodEnd() != null) existing.setPeriodEnd(updatedReport.getPeriodEnd());
        if (updatedReport.getStatus() != null) existing.setStatus(updatedReport.getStatus());
        if (updatedReport.getReportConfig() != null) existing.setReportConfig(updatedReport.getReportConfig());
        
        SavedReport saved = repository.save(existing);
        notifyReportEvent("REPORT_UPDATED", saved, null);
        return saved;
    }

    @Transactional
    public void deleteSavedReport(Long id) {
        SavedReport existing = getSavedReportById(id);
        notifyReportEvent("REPORT_DELETED", existing, null);
        repository.delete(existing);
    }

    @Transactional
    public SavedReport archiveReport(Long id, String reason) {
        SavedReport report = getSavedReportById(id); // Throws RuntimeException (404)
        
        if (report.getStatus() != ReportStatus.GENERATED) {
            throw new IllegalArgumentException("Only GENERATED reports can be archived. Current status: " + report.getStatus());
        }

        report.setStatus(ReportStatus.ARCHIVED);
        Map<String, Object> config = report.getReportConfig();
        if (config == null) {
            config = new java.util.LinkedHashMap<>();
        }
        config.put("archiveReason", reason);
        config.put("archivedAt", LocalDateTime.now().toString());
        report.setReportConfig(config);
        
        SavedReport saved = repository.save(report);
        notifyReportEvent("ARCHIVED", saved, Map.of("reason", reason));
        return saved;
    }

    public UserReportSummaryDTO getUserReportSummary(Long userId) {
        ensureUserExists(userId);

        // Step b & c: Query grouped counts, build typeBreakdown map
        List<Object[]> rows;
        try {
            rows = repository.countGeneratedReportsByType(userId);
        } catch (Exception e) {
            rows = new java.util.ArrayList<>();
        }
        
        long totalReports = repository.countByUserId(userId);
        return userReportSummaryAdapter.adapt(userId, rows, totalReports);
    }

    @Transactional
    public SavedReport generateReport(Long userId, GenerateReportRequestDTO request, boolean simulateFailure) {
        ensureUserExists(userId);

        // 2. Validate period
        if (!request.getPeriodStart().isBefore(request.getPeriodEnd())) {
            throw new IllegalArgumentException("periodStart must be before periodEnd");
        }
        if (repository.existsOverlappingGeneratedReport(userId, request.getReportType(), request.getPeriodStart(), request.getPeriodEnd())) {
            throw new IllegalArgumentException("report already exists");
        }

        SavedReport newReport = new SavedReport();
        newReport.setUserId(userId);
        newReport.setName(request.getName());
        newReport.setReportType(request.getReportType());
        newReport.setPeriodStart(request.getPeriodStart());
        newReport.setPeriodEnd(request.getPeriodEnd());
        newReport.setStatus(ReportStatus.GENERATED);
        
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("reportType", request.getReportType().name());
        config.put("periodStart", request.getPeriodStart().toString());
        config.put("periodEnd", request.getPeriodEnd().toString());
        config.put("generationStatus", "success");
        config.put("comparisonEnabled", false);
        config.put("failureReason", null);
        newReport.setReportConfig(config);

        if (simulateFailure) {
            newReport.setStatus(ReportStatus.FAILED);
            config.put("generationStatus", "failed");
            config.put("failureReason", "Simulated failure");
            SavedReport saved = repository.save(newReport);
            notifyReportEvent("FAILED", saved, null);
            return saved;
        }

        SavedReport saved = repository.save(newReport);
        notifyReportEvent("GENERATED", saved, null);
        return saved;
    }

    @Transactional
    public ReportTemplateUsage applyTemplateToReport(Long reportId, Long templateId) {
        SavedReport report = getSavedReportById(reportId);
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalArgumentException("cannot apply template to a generated/archived report");
        }

        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("ReportTemplate not found with id: " + templateId));

        if (usageRepository.existsBySavedReportIdAndReportTemplateId(reportId, templateId)) {
            throw new IllegalArgumentException("template already applied");
        }

        ReportTemplateUsage usage = new ReportTemplateUsage();
        usage.setSavedReport(report);
        usage.setReportTemplate(template);
        
        // Return the saved usage directly
        ReportTemplateUsage savedUsage = reportTemplateUsageService.createReportTemplateUsage(usage);
        
        if (report.getReportTemplateUsages() != null) {
            report.getReportTemplateUsages().add(savedUsage);
        } else {
            report.setReportTemplateUsages(new java.util.ArrayList<>(java.util.List.of(savedUsage)));
        }
        notifyReportEvent("TEMPLATE_APPLIED", report, null);
        
        return savedUsage;
    }

    @Transactional
    public SavedReport regenerateReport(Long id) {
        SavedReport report = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + id));

        if (report.getStatus() != ReportStatus.FAILED) {
            notifyReportEvent("REGENERATION_DENIED", report, Map.of("reason", "Only FAILED reports can be regenerated."));
            throw new IllegalArgumentException("Only FAILED reports can be regenerated. Current status: " + report.getStatus());
        }

        report.setStatus(ReportStatus.GENERATED);

        Map<String, Object> config = report.getReportConfig();
        if (config == null) {
            config = new java.util.LinkedHashMap<>();
        }

        int retryAttempt = 0;
        if (config.get("retryAttempt") != null) {
            retryAttempt = ((Number) config.get("retryAttempt")).intValue();
        }
        config.put("retryAttempt", retryAttempt + 1);
        config.put("generationStatus", "success");

        report.setReportConfig(config);

        SavedReport saved = repository.save(report);
        notifyReportEvent("REGENERATED", saved, null);
        return saved;
    }
    public ReportDetailsDTO getReportDetails(Long reportId) {
        SavedReport report = repository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + reportId));

        List<ReportTemplateUsage> usages = report.getReportTemplateUsages();

        List<ReportDetailsDTO.AppliedTemplateDTO> appliedTemplates = usages.stream()
                .map(usage -> new ReportDetailsDTO.AppliedTemplateDTO(
                        usage.getReportTemplate().getCode(),
                        usage.getReportTemplate().getTemplateType().name(),
                        usage.getPagesGenerated(),
                        usage.getAppliedAt()
                ))
                .collect(java.util.stream.Collectors.toList());

        Double totalPages = usages.stream()
                .mapToDouble(ReportTemplateUsage::getPagesGenerated)
                .sum();

        return ReportDetailsDTO.builder()
                .reportId(report.getId())
                .userId(report.getUserId())
                .name(report.getName())
                .reportType(report.getReportType())
                .status(report.getStatus())
                .reportConfig(report.getReportConfig())
                .appliedTemplates(appliedTemplates)
                .totalPages(totalPages)
                .templateCount(usages.size())
                .build();
    }

    public ReportAnalyticsDTO getReportAnalytics(LocalDate startDate, LocalDate endDate) {
        // Step a: Validate — both must be provided or both must be absent
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("both startDate and endDate must be provided");
        }
        // Also validate order when both are present
        if (startDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }

        String start = (startDate != null) ? startDate.toString() : null;
        String end   = (endDate != null) ? endDate.toString() : null;

        // Step c: Execute aggregation query
        List<Object[]> results = repository.getReportAnalytics(start, end);
        
        ReportAnalyticsDTO dto = reportAnalyticsAdapter.adapt(results);
        notifyReportEvent("ANALYTICS_VIEWED", null, null);
        return dto;
    }
    
    private void notifyReportEvent(String action, SavedReport report, Map<String, Object> extraDetails) {
        Map<String, Object> payload = new LinkedHashMap<>();
        
        if ("ANALYTICS_VIEWED".equals(action)) {
            payload.put("reportId", -1L);
            payload.put("reportType", null);
            payload.put("pagesGenerated", null);
        } else if (report != null) {
            payload.put("reportId", report.getId());
            payload.put("reportType", report.getReportType() != null ? report.getReportType().name() : null);
            
            double pages = 0.0;
            if (report.getReportTemplateUsages() != null) {
                pages = report.getReportTemplateUsages().stream()
                        .mapToDouble(usage -> usage.getPagesGenerated() != null ? usage.getPagesGenerated() : 0.0)
                        .sum();
            }
            payload.put("pagesGenerated", pages);
            
            Map<String, Object> details = new LinkedHashMap<>();
            if (report.getReportConfig() != null) {
                details.putAll(report.getReportConfig());
            }
            if (extraDetails != null) {
                details.putAll(extraDetails);
            }
            payload.put("details", details);
        }
        
        notifyObservers(action, payload);
    }
    private void ensureUserExists(Long userId) {

        try {
            // Tier 1: Real DB check against shared 'users' table
            if (!repository.existsUserById(userId)) {
                // Secondary check: In case the query is valid but the record is missing
                if (repository.countByUserId(userId) == 0) {
                    throw new RuntimeException("User not found with id: " + userId);
                }
            }
        } catch (Exception e) {
            // Tier 2: SQL Error Fallback (e.g. users table not found in isolated test)
            if (repository.countByUserId(userId) == 0) {
                throw new RuntimeException("User not found with id: " + userId);
            }
        }
    }
}
