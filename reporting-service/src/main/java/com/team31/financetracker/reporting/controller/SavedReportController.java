package com.team31.financetracker.reporting.controller;

import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import com.team31.financetracker.reporting.service.SavedReportService;
import com.team31.financetracker.reporting.service.FinancialHealthService;
import com.team31.financetracker.reporting.config.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.team31.financetracker.reporting.exception.ServiceUnavailableException;


import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

import com.team31.financetracker.reporting.model.ReportType;
import com.team31.financetracker.reporting.model.ReportStatus;
import com.team31.financetracker.reporting.dto.GenerateReportRequestDTO;
import com.team31.financetracker.reporting.dto.ReportAnalyticsDTO;
import com.team31.financetracker.reporting.dto.FinancialHealthScoreDTO;
import com.team31.financetracker.reporting.strategy.RegenerationRequest;
import com.team31.financetracker.reporting.strategy.RegenerationResult;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/reports")
public class SavedReportController {

    private final SavedReportService service;
    private final FinancialHealthService healthService;
    private final JwtService jwtService;

    public SavedReportController(SavedReportService service,
                                 FinancialHealthService healthService,
                                 JwtService jwtService) {
        this.service       = service;
        this.healthService = healthService;
        this.jwtService    = jwtService;
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
        } catch (ResponseStatusException | ServiceUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/generate/{userId}")
    public ResponseEntity<?> generateReport(@PathVariable Long userId, 
                                            @RequestBody GenerateReportRequestDTO request,
                                            @RequestParam(defaultValue = "false") boolean simulateFailure) {
        try {
            SavedReport newReport = service.generateReport(userId, request, simulateFailure);
            return new ResponseEntity<>(newReport, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ResponseStatusException | ServiceUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{reportId}/templates/{templateId}")
    public ResponseEntity<?> applyTemplateToReport(@PathVariable Long reportId, @PathVariable Long templateId) {
        try {
            ReportTemplateUsage usage = service.applyTemplateToReport(reportId, templateId);
            return ResponseEntity.ok(usage);
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

    @GetMapping("/{reportId}/audit")
    public ResponseEntity<?> getReportAuditTrail(@PathVariable Long reportId) {
        return ResponseEntity.ok(service.getReportAuditTrail(reportId));
    }

    @GetMapping("/analytics/audit")
    public ResponseEntity<?> getReportGenerationAudit(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or malformed Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired JWT token");
        }

        String callerRole = jwtService.extractRole(token);
        if (!"ADMIN".equals(callerRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: ADMIN role required");
        }

        try {
            List<com.team31.financetracker.reporting.dto.ReportAuditSummaryDTO> dtos = service.getReportGenerationAudit(startDate, endDate);
            service.logAnalyticsViewed();
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * S5-F10: Get Financial Health Score.
     * Auth: Required (USER). Ownership: uid == userId OR role == ADMIN.
     * Cache: 10 min. MongoDB audit always fires (even on cache hit).
     */
    @GetMapping("/analytics/health")
    public ResponseEntity<?> getFinancialHealthScore(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        // Step a — JWT validation (401)
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or malformed Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired JWT token");
        }

        // Step b — Ownership check (403): uid == userId OR role == ADMIN
        Long callerUid  = jwtService.extractUserId(token);
        String callerRole = jwtService.extractRole(token);
        boolean isOwner = callerUid != null && callerUid.equals(userId);
        boolean isAdmin = "ADMIN".equals(callerRole);
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: you may only view your own health score");
        }

        // Step d (pre-check) — date range validation (400)
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body("startDate must not be after endDate");
        }

        try {
            // Steps c, e, f, g — cached computation
            FinancialHealthScoreDTO dto = healthService.computeHealthScore(userId, startDate, endDate);

            // Step h — audit log OUTSIDE cache (always fires, including on cache hits)
            healthService.logHealthScoreViewed(userId);

            // Step j — return 200
            return ResponseEntity.ok(dto);

        } catch (ServiceUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * S5-F12 — POST /api/reports/{id}/regenerate-report
     *
     * Distinct from M1's PUT /api/reports/{id}/regenerate — both coexist.
     *
     * Request body: { "archivePrevious": true/false, "reason": "..." }
     *
     * Logic order (strict per spec):
     * 1. JWT check          → 401 (handled by security filter automatically)
     * 2. Find report        → 404
     * 3. Validate reason    → 400
     * 4. Select strategy    (no exception yet)
     * 5. NoRegeneration     → 400 "report is archived"
     * 6. Success            → 200 with updated report
     */
    @PostMapping("/{id}/regenerate-report")
    public ResponseEntity<?> regenerateReportWithSnapshot(
            @PathVariable Long id,
            @RequestBody RegenerationRequest request) {

        // Step 3 — Validate reason not blank
        if (request.getReason() == null || request.getReason().isBlank()) {
            return ResponseEntity.badRequest().body("reason must not be blank");
        }

        try {
            RegenerationResult result = service.regenerateReportWithSnapshot(id, request);

            // Step 5 — NoRegenerationStrategy selected → 400
            if ("NoRegenerationStrategy".equals(result.getStrategyName())) {
                return ResponseEntity.badRequest().body("report is archived");
            }

            // Step 6 — Success
            return ResponseEntity.ok(result.getUpdatedReport());

        } catch (RuntimeException e) {
            // getSavedReportById throws RuntimeException when not found
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
