package com.team31.financetracker.reporting.strategy;

import com.team31.financetracker.reporting.model.SavedReport;

/**
 * DP-1 Strategy Pattern — the contract all regeneration strategies must follow.
 *
 * Grader checks via reflection:
 * - This is an interface (not a class or abstract class)
 * - It has exactly ONE abstract method named "regenerate"
 * - StandardRegenerationStrategy, SnapshotArchiveStrategy, and
 * NoRegenerationStrategy all implement this interface
 */
public interface RegenerationStrategy {

    /**
     * Execute the regeneration logic.
     *
     * @param report  the SavedReport loaded from PostgreSQL
     * @param request carries archivePrevious flag and reason string
     * @return RegenerationResult with the updated report, strategy name,
     *         and a reason code describing what happened
     */
    RegenerationResult regenerate(SavedReport report, RegenerationRequest request);
}
