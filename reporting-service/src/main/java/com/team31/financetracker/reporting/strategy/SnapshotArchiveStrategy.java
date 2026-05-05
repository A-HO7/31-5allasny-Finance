package com.team31.financetracker.reporting.strategy;

import com.team31.financetracker.reporting.model.ReportStatus;
import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import com.team31.financetracker.reporting.model.SavedReport;
import com.team31.financetracker.reporting.mongo.ReportSnapshot;
import com.team31.financetracker.reporting.mongo.ReportSnapshotRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DP-1 Strategy — save a snapshot to MongoDB first, then regenerate in place.
 *
 * What it does:
 * 1. Reads current template usages directly from the report object
 * 2. Converts them to MongoDB-safe TemplateUsageSnapshot objects
 * 3. Creates a ReportSnapshot capturing the current state of the report
 * 4. Saves the snapshot to MongoDB (report_snapshots collection)
 * 5. ONLY after snapshot save succeeds — updates the report config
 * 6. Returns the modified report
 *
 * CRITICAL: If the MongoDB snapshot save throws, the exception propagates
 * up and the report is NOT modified. Snapshot must succeed first.
 *
 * Used when: archivePrevious=true AND status is GENERATED or FAILED
 */
@Component
public class SnapshotArchiveStrategy implements RegenerationStrategy {

    // Saves the snapshot to MongoDB report_snapshots collection
    private final ReportSnapshotRepository snapshotRepository;

    // Spring injects this via constructor
    public SnapshotArchiveStrategy(ReportSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    public RegenerationResult regenerate(SavedReport report, RegenerationRequest request) {

        // ── STEP 1: Get template usages from the report object ───────────────
        // No extra repository call needed — SavedReport already has the list
        // via @OneToMany relationship
        List<ReportTemplateUsage> usages = report.getReportTemplateUsages();
        if (usages == null) {
            usages = new java.util.ArrayList<>();
        }

        // ── STEP 2: Convert JPA entities to MongoDB-safe snapshot objects ────
        // We cannot store JPA entities in MongoDB directly.
        // TemplateUsageSnapshot is a simple nested class with just the data.
        List<ReportSnapshot.TemplateUsageSnapshot> snapshotUsages = usages.stream()
            .map(usage -> new ReportSnapshot.TemplateUsageSnapshot(
                usage.getReportTemplate() != null
                    ? usage.getReportTemplate().getCode() : null,
                usage.getReportTemplate() != null
                    ? usage.getReportTemplate().getTemplateType().name() : null,
                usage.getPagesGenerated(),
                usage.getAppliedAt()
            ))
            .collect(Collectors.toList());

        // ── STEP 3: Build the snapshot of the CURRENT state ─────────────────
        // We deep-copy reportConfig so the snapshot captures the state
        // BEFORE we modify it — not after
        ReportSnapshot snapshot = new ReportSnapshot(
            report.getId(),
            report.getStatus().name(),
            report.getPeriodStart(),
            report.getPeriodEnd(),
            copyConfig(report.getReportConfig()),
            snapshotUsages
        );

        // ── STEP 4: Save snapshot to MongoDB ────────────────────────────────
        // If this throws → exception propagates → report is NOT modified
        snapshotRepository.save(snapshot);

        // ── STEP 5: Now modify the report (same logic as Standard) ──────────
        Map<String, Object> config = report.getReportConfig();
        if (config == null) {
            config = new LinkedHashMap<>();
        }

        int regenerationCount = 0;
        Object countObj = config.get("regenerationCount");
        if (countObj != null) {
            regenerationCount = ((Number) countObj).intValue();
        }

        config.put("regenerationCount",       regenerationCount + 1);
        config.put("generationStatus",        "success");
        config.put("lastRegeneratedAt",       LocalDateTime.now().toString());
        config.put("lastRegenerationReason",  request.getReason());
        config.put("lastStrategy",            "SnapshotArchiveStrategy");

        report.setReportConfig(config);
        report.setStatus(ReportStatus.GENERATED);

        // ── STEP 6: Return result ────────────────────────────────────────────
        return new RegenerationResult(
            report,
            "SnapshotArchiveStrategy",
            "snapshot archived and regenerated"
        );
    }

    /**
     * Deep-copies the config map so the snapshot stores the ORIGINAL values,
     * not the modified ones. Without this, both snapshot and report
     * would reference the same map object.
     */
    private Map<String, Object> copyConfig(Map<String, Object> original) {
        if (original == null) return new LinkedHashMap<>();
        return new LinkedHashMap<>(original);
    }
}
