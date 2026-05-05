package com.team31.financetracker.budget.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "budget_events")
public class BudgetEvent implements MongoEvent {

    public static final String USAGE_RECORDED   = "USAGE_RECORDED";
    public static final String ANALYTICS_VIEWED = "ANALYTICS_VIEWED";
    public static final String DASHBOARD_VIEWED = "DASHBOARD_VIEWED";
    public static final String METADATA_UPDATED = "METADATA_UPDATED";
    public static final String BATCH_CREATED    = "BATCH_CREATED";
    public static final String PURGED           = "PURGED";
    public static final String BUDGET_CREATED   = "BUDGET_CREATED";
    public static final String BUDGET_DELETED   = "BUDGET_DELETED";

    @Id
    private String id;

    @Field("budgetId")
    private Long budgetId;

    @Field("action")
    private String action;

    @Field("timestamp")
    private LocalDateTime timestamp;

    @Field("details")
    private Map<String, Object> details;

    public BudgetEvent() {
    }

    public BudgetEvent(Long budgetId, String action, LocalDateTime timestamp, Map<String, Object> details) {
        this.budgetId = budgetId;
        this.action = action;
        this.timestamp = timestamp;
        this.details = details;
    }

    public BudgetEvent(String action, Map<String, Object> details) {
        this.action = action;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}