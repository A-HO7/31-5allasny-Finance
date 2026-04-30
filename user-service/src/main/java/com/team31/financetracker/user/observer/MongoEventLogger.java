package com.team31.financetracker.user.observer;

import com.team31.financetracker.user.factory.EventFactory;
import com.team31.financetracker.user.model.nosql.MongoEvent;
import com.team31.financetracker.user.model.nosql.EventType;
import org.springframework.stereotype.Component;
import com.team31.financetracker.user.model.nosql.AuthEvent;
import com.team31.financetracker.user.repository.nosql.AuthEventRepository;

import java.util.HashMap;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {
    private final AuthEventRepository repository;

    public MongoEventLogger(AuthEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        // Since 'payload' is an Object, we check if it is the Map we expect
        System.out.println("OBSERVER TRIGGERED! Event: " + eventType);
        if (payload instanceof Map) {
            Map<String, Object> details = (Map<String, Object>) payload;
            Long userId = (Long) details.get("userId");
            String action = eventType; // The eventType IS the action

            try {
                Map<String, Object> params = new HashMap<>(details);
                params.put("userId", userId);
                params.put("action", action);

                // Use Factory
                MongoEvent event = EventFactory.createEvent(EventType.AUTH, params);
                repository.save((AuthEvent) event);

                System.out.println("DEBUG: Event saved to MongoDB: " + action);
            } catch (Exception e) {
                System.err.println("WARN: Could not log to MongoDB: " + e.getMessage());
            }
        }
    }
}