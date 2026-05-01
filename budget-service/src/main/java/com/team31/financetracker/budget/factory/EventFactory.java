package com.team31.financetracker.budget.factory;

import com.team31.financetracker.budget.model.BudgetEvent;
import com.team31.financetracker.budget.model.EventType;
import com.team31.financetracker.budget.model.MongoEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {
        String action = params.get("action") != null
                ? params.get("action").toString()
                : "UNKNOWN";

        Map<String, Object> details = new HashMap<>(params);
        details.remove("action");

        return switch (type) {
            case BUDGET -> new BudgetEvent(action, details);
            default -> throw new IllegalArgumentException("Unsupported event type in budget-service: " + type);
        };
    }
}