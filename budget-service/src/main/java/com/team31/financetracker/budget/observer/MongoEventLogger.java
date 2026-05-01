package com.team31.financetracker.budget.observer;

import com.team31.financetracker.budget.factory.EventFactory;
import com.team31.financetracker.budget.model.BudgetEvent;
import com.team31.financetracker.budget.model.EventType;
import com.team31.financetracker.budget.model.MongoEvent;
import com.team31.financetracker.budget.repository.BudgetEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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
            Map<String, Object> params = new HashMap<>();
            params.put("action", eventType);

            if (payload instanceof Map<?, ?> map) {
                map.forEach((key, value) -> params.put(String.valueOf(key), value));
            } else if (payload != null) {
                params.put("payload", payload.toString());
            }

            MongoEvent event = eventFactory.createEvent(EventType.BUDGET, params);
            budgetEventRepository.save((BudgetEvent) event);

        } catch (Exception e) {
            log.warn("Failed to write BudgetEvent to MongoDB", e);
        }
    }
}