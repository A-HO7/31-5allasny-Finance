package com.team31.financetracker.reporting.controller;

import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import com.team31.financetracker.reporting.service.ReportTemplateUsageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports/usages")
public class ReportTemplateUsageController {

    private final ReportTemplateUsageService service;

    public ReportTemplateUsageController(ReportTemplateUsageService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> createReportTemplateUsage(@RequestBody ReportTemplateUsage usage) {
        try {
            return new ResponseEntity<>(service.createReportTemplateUsage(usage), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Failed mapping inner relations logic. " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<ReportTemplateUsage>> getAllReportTemplateUsages() {
        return ResponseEntity.ok(service.getAllReportTemplateUsages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportTemplateUsage> getReportTemplateUsageById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getReportTemplateUsageById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReportTemplateUsage(@PathVariable Long id, @RequestBody ReportTemplateUsage usage) {
        try {
            ReportTemplateUsage updated = service.updateReportTemplateUsage(id, usage);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReportTemplateUsage(@PathVariable Long id) {
        try {
            service.deleteReportTemplateUsage(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
