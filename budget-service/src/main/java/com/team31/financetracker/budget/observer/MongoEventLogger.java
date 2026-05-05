package com.team31.financetracker.budget.observer;

import com.team31.financetracker.budget.factory.EventFactory;
import com.team31.financetracker.budget.model.BudgetEvent;
import com.team31.financetracker.budget.model.MongoEvent;
import com.team31.financetracker.budget.repository.BudgetEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Observer Pattern (DP-2) — concrete observer that persists a BudgetEvent
 * to MongoDB whenever a subject calls onEvent(String, Object).
 *
 * Failure policy: Any MongoDB exception is caught, logged at WARN level,
 * and NOT re-thrown. The upstream database write must not be rolled back on
 * a Mongo write failure.
 */
@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final EventFactory eventFactory;
    private final BudgetEventRepository budgetEventRepository;

    public MongoEventLogger(EventFactory eventFactory,
                            BudgetEventRepository budgetEventRepository) {
        this.eventFactory = eventFactory;
        this.budgetEventRepository = budgetEventRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> details = new HashMap<>();

            if (payload instanceof Map<?, ?> map) {
                map.forEach((key, value) -> details.put(String.valueOf(key), value));
            } else if (payload != null) {
                details.put("payload", payload.toString());
            }

            Long budgetId = extractBudgetId(details);

            MongoEvent event = eventFactory.createEvent(budgetId, eventType, details);

            budgetEventRepository.save((BudgetEvent) event);

        } catch (Exception ex) {
            log.warn("MongoEventLogger failed to persist event [{}]: {}", eventType, ex.getMessage(), ex);
        }
    }

    private Long extractBudgetId(Map<String, Object> details) {
        Object rawBudgetId = details.get("budgetId");

        if (rawBudgetId instanceof Number number) {
            return number.longValue();
        }

        if (rawBudgetId != null) {
            try {
                return Long.parseLong(rawBudgetId.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}