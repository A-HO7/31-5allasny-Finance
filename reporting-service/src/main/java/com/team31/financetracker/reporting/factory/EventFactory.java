package com.team31.financetracker.reporting.factory;

import com.team31.financetracker.reporting.mongo.ReportAuditEvent;
import com.team31.financetracker.reporting.observer.MongoEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating MongoDB event objects (DP-6 — Factory Pattern).
 *
 * Dispatches on EventType to return the correct concrete MongoEvent subtype.
 * Services must never use `new ReportAuditEvent(...)` directly — all event
 * construction flows through this factory.
 *
 * Composition with Observer (DP-2):
 *   Write endpoint → notifyObservers(action, payload)
 *     → MongoEventLogger.onEvent(action, payload)
 *       → EventFactory.createEvent(REPORT_AUDIT, params)
 *         → ReportAuditEvent persisted via ReportAuditEventRepository
 */
public class EventFactory {

    private EventFactory() {}

    /**
     * Creates the appropriate MongoEvent subtype for the given EventType.
     *
     * Expected params keys (all services):
     *   "action"         — String, UPPER_SNAKE_CASE action identifier
     *   "timestamp"      — LocalDateTime (defaults to now if absent)
     *   "details"        — Map<String,Object> (optional additional context)
     *
     * Reporting-service specific keys (for REPORT_AUDIT):
     *   "reportId"       — Long
     *   "reportType"     — String (null only for ANALYTICS_VIEWED)
     *   "pagesGenerated" — Double (null only for ANALYTICS_VIEWED)
     *
     * @param type   the event type that determines which concrete class to create
     * @param params key-value map carrying the event data
     * @return a populated MongoEvent ready to be persisted
     */
    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {
        return switch (type) {
            case REPORT_AUDIT -> buildReportAuditEvent(params);
            // Other cases will be added when the shared factory is needed cross-service.
            // For now only the reporting-service uses this factory instance.
            default -> throw new IllegalArgumentException("Unsupported EventType: " + type);
        };
    }

    // ── Private builders ─────────────────────────────────────────────────────

    private static ReportAuditEvent buildReportAuditEvent(Map<String, Object> params) {
        ReportAuditEvent event = new ReportAuditEvent();

        event.setAction(getString(params, "action"));
        event.setTimestamp(getTimestamp(params));
        event.setReportId(getLong(params, "reportId"));
        event.setReportType(getString(params, "reportType"));   // null on ANALYTICS_VIEWED
        event.setPagesGenerated(getDouble(params, "pagesGenerated")); // null on ANALYTICS_VIEWED

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) params.get("details");
        event.setDetails(details != null ? details : new HashMap<>());

        // Strict null constraints
        if (event.getAction() == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (event.getReportId() == null) {
            throw new IllegalArgumentException("reportId must not be null");
        }

        // Conditional requirements for report-shaped actions
        if (!"ANALYTICS_VIEWED".equals(event.getAction())) {
            if (event.getReportType() == null) {
                throw new IllegalArgumentException("reportType is required for report-shaped action: " + event.getAction());
            }
            if (event.getPagesGenerated() == null) {
                throw new IllegalArgumentException("pagesGenerated is required for report-shaped action: " + event.getAction());
            }
        }

        return event;
    }

    // ── Safe param extractors ────────────────────────────────────────────────

    private static String getString(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val != null ? val.toString() : null;
    }

    private static Long getLong(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }

    private static Double getDouble(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) return null;
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(val.toString());
    }

    private static LocalDateTime getTimestamp(Map<String, Object> params) {
        Object val = params.get("timestamp");
        if (val instanceof LocalDateTime ldt) return ldt;
        return LocalDateTime.now();
    }
}
