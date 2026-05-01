package com.team31.financetracker.budget.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "budget_events")
public class BudgetEvent implements MongoEvent {

    @Id
    private String id;

    private LocalDateTime timestamp;
    private String action;
    private Map<String, Object> details = new HashMap<>();

    public BudgetEvent() {
    }

    public BudgetEvent(String action, Map<String, Object> details) {
        this.timestamp = LocalDateTime.now();
        this.action = action;
        this.details = details != null ? details : new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}