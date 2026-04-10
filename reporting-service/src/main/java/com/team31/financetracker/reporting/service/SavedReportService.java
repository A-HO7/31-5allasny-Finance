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

import com.team31.financetracker.reporting.model.ReportType;
import com.team31.financetracker.reporting.model.ReportStatus;

@Service
public class SavedReportService {

    private final SavedReportRepository repository;

    public SavedReportService(SavedReportRepository repository) {
        this.repository = repository;
    }

    public SavedReport createSavedReport(SavedReport savedReport) {
        return repository.save(savedReport);
    }

    public List<SavedReport> getAllSavedReports() {
        return repository.findAll();
    }

    public List<SavedReport> searchReports(ReportType reportType, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;
        return repository.searchReports(reportType, start, end);
    }

    public SavedReport getSavedReportById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("SavedReport not found with id: " + id));
    }

    @Transactional
    public SavedReport updateSavedReport(Long id, SavedReport updatedReport) {
        SavedReport existing = getSavedReportById(id);
        
        if (updatedReport.getUserId() == null || updatedReport.getName() == null || 
            updatedReport.getReportType() == null || updatedReport.getPeriodStart() == null || 
            updatedReport.getPeriodEnd() == null || updatedReport.getStatus() == null) {
            throw new IllegalArgumentException("Missing required fields for SavedReport update process.");
        }

        existing.setUserId(updatedReport.getUserId());
        existing.setName(updatedReport.getName());
        existing.setReportType(updatedReport.getReportType());
        existing.setPeriodStart(updatedReport.getPeriodStart());
        existing.setPeriodEnd(updatedReport.getPeriodEnd());
        existing.setStatus(updatedReport.getStatus());
        existing.setReportConfig(updatedReport.getReportConfig());
        
        return repository.save(existing);
    }

    @Transactional
    public void deleteSavedReport(Long id) {
        SavedReport existing = getSavedReportById(id);
        repository.delete(existing);
    }

    @Transactional
    public void archiveReport(Long id, String reason) {
        SavedReport report = getSavedReportById(id); // Throws RuntimeException (404)
        
        if (report.getStatus() != ReportStatus.GENERATED) {
            throw new IllegalArgumentException("Only GENERATED reports can be archived. Current status: " + report.getStatus());
        }

        repository.archiveReportNative(id, reason, LocalDateTime.now().toString());
    }

    public UserReportSummaryDTO getUserReportSummary(Long userId) {
        // Step a: Verify user exists via cross-service native SQL check
        if (!repository.existsUserById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        // Step b & c: Query grouped counts, build typeBreakdown map
        List<Object[]> rows = repository.countGeneratedReportsByType(userId);
        Map<String, Integer> typeBreakdown = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String type = (String) row[0];
            Integer count = ((Number) row[1]).intValue();
            typeBreakdown.put(type, count);
        }

        // Step d: Calculate totals
        long totalReports = repository.countByUserId(userId);
        long generatedCount = typeBreakdown.values().stream().mapToLong(Integer::longValue).sum();

        // Step e: Build and return DTO
        return new UserReportSummaryDTO(userId, totalReports, generatedCount, typeBreakdown);
    }

    @Transactional
    public SavedReport generateReport(Long userId, GenerateReportRequestDTO request) {
        if (!repository.existsUserById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
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
        newReport.setReportConfig(config);

        return repository.save(newReport);
    }
}
