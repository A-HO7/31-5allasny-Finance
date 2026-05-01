package com.team31.financetracker.user.model.nosql;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "account_events")
public class AccountEvent implements MongoEvent {
    @Id
    private String id;
    private Long accountId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public AccountEvent(Long accountId, String action, Map<String, Object> details) {
        this.accountId = accountId;
        this.action = action;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    @Override public String getId() { return id; }
    @Override public LocalDateTime getTimestamp() { return timestamp; }
    @Override public String getAction() { return action; }
    @Override public Map<String, Object> getDetails() { return details; }
}
