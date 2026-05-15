package com.team31.financetracker.transaction.Enums;

public enum TransactionStatus {
    PENDING,
    COMPLETED,
    APPROVED,
    VOIDED,
    // ── M3 Saga states ──────────────────────────────────────────────────────
    /** Saga in-flight: transaction.completed published, waiting for report.initiated */
    COMPLETING,
    /** report.initiated consumed: waiting for report.completed / report.failed */
    REPORT_PENDING,
    /** report.completed consumed: saga final-success state */
    REPORTED,
    /** report.failed consumed: compensation in progress */
    REPORT_FAILED,
    /** report.reverted consumed: saga compensated */
    REVERTED
}
