package com.team31.financetracker.user.observer;

import com.team31.financetracker.user.factory.EventFactory;
import com.team31.financetracker.user.model.nosql.MongoEvent;
import com.team31.financetracker.user.model.nosql.EventType;
import org.springframework.stereotype.Component;
import com.team31.financetracker.user.model.nosql.AuthEvent;
import com.team31.financetracker.user.repository.nosql.AuthEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {
    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);
    private final AuthEventRepository repository;

    public MongoEventLogger(AuthEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        // Since 'payload' is an Object, we check if it is the Map we expect
        if (payload instanceof Map) {
            Map<String, Object> details = (Map<String, Object>) payload;
            Long userId = (Long) details.get("userId");
            String action = eventType; // The eventType IS the action

            try {
                Map<String, Object> params = new HashMap<>(details);
                params.put("userId", userId);
                params.put("action", action);
                
                EventType type;
                try {
                    type = EventType.valueOf(eventType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Fallback to AUTH if it's an old string like "USER_CREATED"
                    type = EventType.AUTH;
                }

                // Use Factory
                MongoEvent event = EventFactory.createEvent(type, params);
                
                if (event instanceof AuthEvent) {
                    repository.save((AuthEvent) event);
                }

            } catch (Exception e) {
                log.warn("Could not log to MongoDB: {}", e.getMessage());
            }
        }
    }
}