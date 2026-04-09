package com.team31.financetracker.reporting.service;

import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.repository.SavedReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public SavedReport getSavedReportById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("SavedReport not found with id: " + id));
    }

    @Transactional
    public SavedReport updateSavedReport(Long id, SavedReport updatedReport) {
        SavedReport existing = getSavedReportById(id);
        
        // Manual check for strictly required fields per specification
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
}
