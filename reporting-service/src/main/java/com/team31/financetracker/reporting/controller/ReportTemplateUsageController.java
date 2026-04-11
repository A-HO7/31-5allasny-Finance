package com.team31.financetracker.reporting.controller;

import com.team31.financetracker.reporting.dto.ReportTemplateUsageDTO;
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

    @GetMapping
    public ResponseEntity<List<ReportTemplateUsageDTO>> getAllReportTemplateUsages() {
        return ResponseEntity.ok(service.getAllReportTemplateUsages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportTemplateUsageDTO> getReportTemplateUsage(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getReportTemplateUsage(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ReportTemplateUsageDTO> createReportTemplateUsage(@RequestBody ReportTemplateUsageDTO dto) {
        ReportTemplateUsageDTO created = service.createReportTemplateUsage(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReportTemplateUsageDTO> updateReportTemplateUsage(@PathVariable Long id, @RequestBody ReportTemplateUsageDTO dto) {
        try {
            return ResponseEntity.ok(service.updateReportTemplateUsage(id, dto));
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
