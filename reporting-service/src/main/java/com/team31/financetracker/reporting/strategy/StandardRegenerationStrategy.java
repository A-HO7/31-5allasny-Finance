package com.team31.financetracker.reporting.strategy;

import com.team31.financetracker.reporting.model.ReportStatus;
import com.team31.financetracker.reporting.model.SavedReport;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DP-1 Strategy — regenerate in place, no snapshot.
 *
 * What it does:
 * 1. Reads current regenerationCount from reportConfig (defaults to 0 if missing)
 * 2. Increments by 1
 * 3. Updates reportConfig with new fields
 * 4. Sets status to GENERATED
 * 5. Returns the modified report — the SERVICE saves it to PostgreSQL
 *
 * Used when: archivePrevious=false AND status is GENERATED or FAILED
 */
@Component
public class StandardRegenerationStrategy implements RegenerationStrategy {

    @Override
    public RegenerationResult regenerate(SavedReport report, RegenerationRequest request) {

        // Step 1 — get or initialize the config map
        Map<String, Object> config = report.getReportConfig();
        if (config == null) {
            config = new LinkedHashMap<>();
        }

        // Step 2 — read current count, default to 0 if absent
        int regenerationCount = 0;
        Object countObj = config.get("regenerationCount");
        if (countObj != null) {
            regenerationCount = ((Number) countObj).intValue();
        }

        // Step 3 — update all required config fields
        config.put("regenerationCount",       regenerationCount + 1);
        config.put("generationStatus",        "success");
        config.put("lastRegeneratedAt",       LocalDateTime.now().toString());
        config.put("lastRegenerationReason",  request.getReason());
        config.put("lastStrategy",            "StandardRegenerationStrategy");

        // Step 4 — apply to report
        report.setReportConfig(config);
        report.setStatus(ReportStatus.GENERATED);

        // Step 5 — return result (service handles the actual save)
        return new RegenerationResult(
            report,
            "StandardRegenerationStrategy",
            "regenerated in place"
        );
    }
}
