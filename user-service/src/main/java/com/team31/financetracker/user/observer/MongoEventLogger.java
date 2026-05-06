package com.team31.financetracker.user.observer;

import com.team31.financetracker.user.factory.EventFactory;
import com.team31.financetracker.user.model.nosql.MongoEvent;
import com.team31.financetracker.user.model.nosql.EventType;
import com.team31.financetracker.user.model.nosql.AuthEvent;
import com.team31.financetracker.user.repository.nosql.AuthEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
    @SuppressWarnings("unchecked")
    public void onEvent(String eventType, Object payload) {
        // Soft Dependency requirement: wrap everything in a try-catch
        try {
            if (!(payload instanceof Map)) {
                log.warn("MongoEventLogger received unexpected payload type: {}",
                        payload != null ? payload.getClass().getName() : "null");
                return;
            }

            Map<String, Object> details = (Map<String, Object>) payload;

            // 1. Prepare parameters for the Factory
            Map<String, Object> params = new HashMap<>(details);

            // 2. Map the action string (e.g., "REGISTERED", "ROLE_CHANGED")
            params.put("action", eventType);

            // 3. Robust Numeric Handling for userId
            // Jackson and Maps often convert IDs to Integer; direct cast to (Long) fails.
            Object userIdObj = details.get("userId");
            if (userIdObj instanceof Number num) {
                params.put("userId", num.longValue());
            }

            // 4. Call the Factory
            // Note: The User Service ONLY uses the AUTH EventType.
            // The specific action (REGISTERED, LOGIN) is passed inside params.
            MongoEvent event = EventFactory.createEvent(EventType.AUTH, params);

            // 5. Save to MongoDB
            if (event instanceof AuthEvent authEvent) {
                repository.save(authEvent);
            }

        } catch (Exception e) {
            // Requirement Section 3.3: log at WARN level and DO NOT rethrow.
            // This ensures Postgres transactions aren't rolled back if Mongo fails.
            log.warn("Mongo Logging Failed for event '{}': {}", eventType, e.getMessage());
        }
    }
}