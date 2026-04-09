package com.team31.financetracker.reporting.controller;

import com.team31.financetracker.reporting.model.ReportTemplate;
import com.team31.financetracker.reporting.service.ReportTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports/templates")
public class ReportTemplateController {

    private final ReportTemplateService service;

    public ReportTemplateController(ReportTemplateService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReportTemplate> createReportTemplate(@RequestBody ReportTemplate template) {
        return new ResponseEntity<>(service.createReportTemplate(template), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ReportTemplate>> getAllReportTemplates() {
        return ResponseEntity.ok(service.getAllReportTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportTemplate> getReportTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getReportTemplateById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReportTemplate(@PathVariable Long id, @RequestBody ReportTemplate template) {
        try {
            ReportTemplate updated = service.updateReportTemplate(id, template);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReportTemplate(@PathVariable Long id) {
        try {
            service.deleteReportTemplate(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
