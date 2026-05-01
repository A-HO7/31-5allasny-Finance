package com.team31.financetracker.budget.model;

import java.time.LocalDateTime;
import java.util.Map;

public interface MongoEvent {
    String getId();

    LocalDateTime getTimestamp();

    String getAction();

    Map<String, Object> getDetails();
}