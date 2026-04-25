package com.team31.financetracker.reporting.controller;

import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import com.team31.financetracker.reporting.service.SavedReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

import com.team31.financetracker.reporting.model.ReportType;
import com.team31.financetracker.reporting.model.ReportStatus;
import com.team31.financetracker.reporting.dto.GenerateReportRequestDTO;
import com.team31.financetracker.reporting.dto.ReportAnalyticsDTO;

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
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.searchReports(reportType, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavedReport> getSavedReportById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getSavedReportById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
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
    public ResponseEntity<?> archiveReport(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        try {
            String reason = (body != null) ? body.get("reason") : null;
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Archive reason is required");
            }
            SavedReport archived = service.archiveReport(id, reason);
            return ResponseEntity.ok(archived);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{id}/summary")
    public ResponseEntity<?> getUserReportSummary(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getUserReportSummary(id));
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
    public ResponseEntity<ReportTemplateUsage> applyTemplateToReport(@PathVariable Long reportId, @PathVariable Long templateId) {
        try {
            ReportTemplateUsage usage = service.applyTemplateToReport(reportId, templateId);
            return ResponseEntity.ok(usage);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
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

    @PutMapping("/{id}/regenerate")
    public ResponseEntity<?> regenerateReport(@PathVariable Long id) {
        try {
            SavedReport updated = service.regenerateReport(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getReportAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ResponseEntity.ok(service.getReportAnalytics(startDate, endDate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
