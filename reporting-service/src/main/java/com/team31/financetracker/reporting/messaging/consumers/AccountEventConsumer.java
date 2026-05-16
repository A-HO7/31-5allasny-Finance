package com.team31.financetracker.reporting.messaging.consumers;

import com.team31.financetracker.contracts.events.AccountRatedEvent;
import com.team31.financetracker.contracts.events.AccountStatsUpdatedEvent;
import com.team31.financetracker.reporting.config.ReportEventConfig;
import com.team31.financetracker.reporting.observer.EntityObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S5-EVENTS — Task 5: Observability Consumers.
 *
 * Listens for account-level events to enrich the report_audit_trail MongoDB
 * collection for S5-F11 analytics. These are NOT part of the saga — disabling
 * them does not break the Transaction Completion Saga.
 *
 * AccountRatedEvent fields:   accountId, statementId, rating
 * AccountStatsUpdatedEvent:   accountId, transactionDelta, balanceDelta
 */
@Component
public class AccountEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);

    private final List<EntityObserver> observers;

    public AccountEventConsumer(List<EntityObserver> observers) {
        this.observers = observers;
    }

    // ─── account.rated ───────────────────────────────────────────────────────

    @RabbitHandler
    public void onAccountRated(AccountRatedEvent event) {
        MDC.put("routingKey", ReportEventConfig.RK_ACCOUNT_RATED);
        MDC.put("accountId", String.valueOf(event.accountId()));

        log.info("Received {} accountId={} statementId={} rating={}",
                ReportEventConfig.RK_ACCOUNT_RATED, event.accountId(),
                event.statementId(), event.rating());

        try {
            // Plan: details = { accountId, statementId, rating }
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("accountId",   event.accountId());
            details.put("statementId", event.statementId());
            details.put("rating",      event.rating());

            // Plan: action = "RATED", reportType and pagesGenerated = null
            notifyObserversSoft("RATED", details);
        } finally {
            MDC.remove("routingKey");
            MDC.remove("accountId");
        }
    }

    // ─── account.stats-updated ───────────────────────────────────────────────

    @RabbitHandler
    public void onAccountStatsUpdated(AccountStatsUpdatedEvent event) {
        MDC.put("routingKey", ReportEventConfig.RK_ACCOUNT_STATS);
        MDC.put("accountId", String.valueOf(event.accountId()));

        log.info("Received {} accountId={} transactionDelta={} balanceDelta={}",
                ReportEventConfig.RK_ACCOUNT_STATS, event.accountId(),
                event.transactionDelta(), event.balanceDelta());

        try {
            // Plan: details = { accountId, transactionDelta, balanceDelta }
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("accountId",        event.accountId());
            details.put("transactionDelta", event.transactionDelta());
            details.put("balanceDelta",     event.balanceDelta());

            // Plan: action = "STATS_UPDATED", reportType and pagesGenerated = null
            notifyObserversSoft("STATS_UPDATED", details);
        } finally {
            MDC.remove("routingKey");
            MDC.remove("accountId");
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    /**
     * Fires the Observer chain. MongoDB failure is always caught and logged at WARN.
     * Plan explicitly states: best-effort only — if MongoDB is unavailable, do not rethrow.
     * reportType and pagesGenerated are null (not a report-lifecycle action).
     */
    private void notifyObserversSoft(String action, Map<String, Object> details) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("reportId",       -1L);
            payload.put("reportType",     null);   // not a report-lifecycle action
            payload.put("pagesGenerated", null);   // not a report-lifecycle action
            payload.put("details",        details);

            for (EntityObserver observer : observers) {
                observer.onEvent(action, payload);
            }
        } catch (Exception e) {
            log.warn("Observability observer failed for action='{}': {}", action, e.getMessage());
        }
    }
}
