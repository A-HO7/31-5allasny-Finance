package com.team31.financetracker.reporting.observer;

import com.team31.financetracker.reporting.factory.EventFactory;
import com.team31.financetracker.reporting.factory.EventType;
import com.team31.financetracker.reporting.mongo.ReportAuditEvent;
import com.team31.financetracker.reporting.mongo.ReportAuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.team31.financetracker.reporting.util.RedisCacheEvictor;

/**
 * Concrete GoF Observer (DP-2) that persists events to MongoDB.
 *
 * Each of the 5 services owns its own MongoEventLogger instance bound to a
 * fixed EventType at construction time. For reporting-service: REPORT_AUDIT.
 *
 * Failure policy: Any MongoDB exception is caught, logged at WARN level,
 * and NOT rethrown. The upstream PostgreSQL transaction must not be rolled
 * back due to a MongoDB write failure.
 *
 * This class is NOT wired to the Observer subject yet — wiring happens in
 * Member B Phase 2 when SavedReportService.register() is called.
 */
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private static final Set<String> DATA_MUTATING = Set.of(
            "GENERATED", "FAILED", "REGENERATED", "REGENERATION_DENIED", "ARCHIVED", "TEMPLATE_APPLIED"
    );

    private final EventType boundEventType;
    private final ReportAuditEventRepository repository;
    private final RedisCacheEvictor redisCacheEvictor;

    /**
     * @param boundEventType the fixed EventType this logger is bound to (REPORT_AUDIT for reporting-service)
     * @param repository     Spring Data MongoDB repository for persisting events
     * @param redisCacheEvictor For wildcard invalidation on data-mutating events
     */
    public MongoEventLogger(EventType boundEventType, 
                            ReportAuditEventRepository repository,
                            RedisCacheEvictor redisCacheEvictor) {
        this.boundEventType = boundEventType;
        this.repository = repository;
        this.redisCacheEvictor = redisCacheEvictor;
    }

    /**
     * Called by the subject (SavedReportService) after every state-changing operation.
     *
     * The action string is NOT used to infer EventType — this logger is already
     * bound to REPORT_AUDIT. The action string is copied into params and passed to
     * EventFactory so it ends up on the persisted document.
     *
     * @param eventType the action string in UPPER_SNAKE_CASE (e.g. "GENERATED")
     * @param payload   domain object or Map carrying event-specific data
     */
    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> params = buildParams(eventType, payload);
            ReportAuditEvent event = (ReportAuditEvent) EventFactory.createEvent(boundEventType, params);
            repository.save(event);
            
            // Invalidate analytics caches on data-mutating events only.
            // Exclude ANALYTICS_VIEWED / DASHBOARD_VIEWED to avoid self-defeating loops.
            if (DATA_MUTATING.contains(eventType)) {
                redisCacheEvictor.evictByPatterns("reporting-service::S5-F10::*");
            }
        } catch (Exception e) {
            // Soft dependency: log and continue — never rethrow to the upstream PG transaction
            log.warn("MongoEventLogger failed to persist event '{}': {}", eventType, e.getMessage(), e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Builds the factory params map from the action string and payload.
     * Member B Phase 2 will enrich the payload with reportId, reportType,
     * pagesGenerated, and details before calling notifyObservers().
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildParams(String eventType, Object payload) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", eventType);
        params.put("timestamp", LocalDateTime.now());

        // If payload is already a map (the expected contract from notifyObservers calls)
        if (payload instanceof Map<?, ?> rawMap) {
            params.putAll((Map<String, Object>) rawMap);
        }

        return params;
    }
}
