package com.team31.financetracker.user.factory;

import com.team31.financetracker.user.model.nosql.*;
import java.util.Map;

public class EventFactory {
    public static MongoEvent createEvent(String type, Map<String, Object> params) {
        return switch (type) {
            case "AUTH" -> (MongoEvent) new AuthEvent((Long)params.get("userId"), (String)params.get("action"), params);

            case "USER_UPDATED", "USER_DEACTIVATED" -> (MongoEvent) new AuthEvent((Long)params.get("userId"), (String)params.get("action"), params);

            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }
}