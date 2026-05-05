package com.team31.financetracker.reporting.strategy;

import com.team31.financetracker.reporting.model.SavedReport;

/**
 * What a strategy returns after executing.
 *
 * updatedReport — the modified SavedReport ready to be saved to PostgreSQL.
 *                 NULL when NoRegenerationStrategy runs (no changes made).
 *
 * strategyName  — simple class name of the strategy that ran.
 *                 e.g. "StandardRegenerationStrategy"
 *                 The service checks this string to decide what to do next.
 *                 Also stored in reportConfig.lastStrategy and audit event.
 *
 * reasonCode    — short description of the outcome.
 *                 e.g. "regenerated in place"
 *                 or   "report is archived and cannot be regenerated"
 */
public class RegenerationResult {

    private SavedReport updatedReport;
    private String strategyName;
    private String reasonCode;

    public RegenerationResult() {}

    public RegenerationResult(SavedReport updatedReport,
                               String strategyName,
                               String reasonCode) {
        this.updatedReport = updatedReport;
        this.strategyName  = strategyName;
        this.reasonCode    = reasonCode;
    }

    public SavedReport getUpdatedReport() { return updatedReport; }
    public void setUpdatedReport(SavedReport updatedReport) {
        this.updatedReport = updatedReport;
    }

    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }
}
