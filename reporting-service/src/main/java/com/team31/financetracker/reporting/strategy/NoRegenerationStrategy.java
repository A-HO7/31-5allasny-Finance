package com.team31.financetracker.reporting.strategy;

import com.team31.financetracker.reporting.model.SavedReport;
import org.springframework.stereotype.Component;

/**
 * DP-1 Strategy — refuse regeneration because the report is ARCHIVED.
 *
 * What it does:
 * Returns a RegenerationResult with:
 * - updatedReport = null  (no changes to the report)
 * - strategyName  = "NoRegenerationStrategy"
 * - reasonCode    = the exact spec-required message
 *
 * The SERVICE reads strategyName, writes the REGENERATION_DENIED audit event,
 * evicts caches, then returns 400. This strategy itself never throws.
 *
 * Used when: report status is ARCHIVED (any value of archivePrevious)
 */
@Component
public class NoRegenerationStrategy implements RegenerationStrategy {

    // Exact string the spec requires in the audit event and 400 response
    public static final String REASON_CODE =
        "report is archived and cannot be regenerated";

    @Override
    public RegenerationResult regenerate(SavedReport report, RegenerationRequest request) {
        // No changes — just signal denial back to the service
        return new RegenerationResult(
            null,
            "NoRegenerationStrategy",
            REASON_CODE
        );
    }
}
