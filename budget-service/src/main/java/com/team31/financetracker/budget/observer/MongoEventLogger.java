package com.team31.financetracker.budget.observer;

import com.team31.financetracker.budget.model.BudgetEvent;
import com.team31.financetracker.budget.repository.BudgetEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Observer Pattern (DP-2) — concrete observer that persists a {@link BudgetEvent}
 * to MongoDB whenever a subject calls {@link #onEvent(String, Object)}.
 *
 * <p><b>Failure policy:</b> Any MongoDB exception is caught, logged at WARN level,
 * and NOT re-thrown. The upstream Cassandra write must not be rolled back on a
 * Mongo write failure.
 */
@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final BudgetEventRepository eventRepository;

    public MongoEventLogger(BudgetEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> details = new HashMap<>();
            Long budgetId = null;

            if (payload instanceof Map<?, ?> rawMap) {
                Map<String, Object> map = (Map<String, Object>) rawMap;
                Object bid = map.get("budgetId");
                if (bid instanceof Number n) budgetId = n.longValue();
                details.put("spent_amount", map.get("spentAmount"));
                details.put("percent_used", map.get("percentUsed"));
                details.put("category", map.get("category"));
            }

            BudgetEvent event = new BudgetEvent(budgetId, eventType, LocalDateTime.now(), details);
            eventRepository.save(event);
        } catch (Exception ex) {
            log.warn("MongoEventLogger failed to persist event [{}]: {}", eventType, ex.getMessage(), ex);
        }
    }
}
