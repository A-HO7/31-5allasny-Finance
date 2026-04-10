package com.team31.financetracker.reporting.controller;

import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.service.SavedReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

import com.team31.financetracker.reporting.model.ReportType;
import com.team31.financetracker.reporting.dto.GenerateReportRequestDTO;

@RestController
@RequestMapping("/api/reports")
public class SavedReportController {

    private final SavedReportService service;

    public SavedReportController(SavedReportService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SavedReport> createSavedReport(@RequestBody SavedReport savedReport) {
        return new ResponseEntity<>(service.createSavedReport(savedReport), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SavedReport>> getAllSavedReports() {
        return ResponseEntity.ok(service.getAllSavedReports());
    }

    @GetMapping("/search")
    public ResponseEntity<List<SavedReport>> searchReports(
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.searchReports(reportType, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavedReport> getSavedReportById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSavedReportById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSavedReport(@PathVariable Long id, @RequestBody SavedReport savedReport) {
        try {
            SavedReport updated = service.updateSavedReport(id, savedReport);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSavedReport(@PathVariable Long id) {
        try {
            service.deleteSavedReport(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archiveReport(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        try {
            String reason = body.getOrDefault("reason", "No reason provided");
            service.archiveReport(id, reason);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<?> getUserReportSummary(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(service.getUserReportSummary(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/generate/{userId}")
    public ResponseEntity<?> generateReport(@PathVariable Long userId, @RequestBody GenerateReportRequestDTO request) {
        try {
            SavedReport newReport = service.generateReport(userId, request);
            return new ResponseEntity<>(newReport, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{reportId}/templates/{templateId}")
    public ResponseEntity<?> applyTemplateToReport(@PathVariable Long reportId, @PathVariable Long templateId) {
        try {
            SavedReport updated = service.applyTemplateToReport(reportId, templateId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/{reportId}/details")
    public ResponseEntity<?> getReportDetails(@PathVariable Long reportId) {
        try {
            return ResponseEntity.ok(service.getReportDetails(reportId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
