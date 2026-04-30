package com.team31.financetracker.user.factory;

import com.team31.financetracker.user.model.nosql.*;
import java.util.Map;

public class EventFactory {
    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {
        return switch (type) {
            case AUTH -> {
                Long userId = null;
                if (params.containsKey("userId") && params.get("userId") != null) {
                    userId = ((Number) params.get("userId")).longValue();
                }
                String action = (String) params.get("action");
                yield new AuthEvent(userId, action, params);
            }
            default -> throw new UnsupportedOperationException("Event type " + type + " is not supported in the User Service yet.");
        };
    }
}