package com.team31.financetracker.account.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "account_events")
public class AccountEvent implements MongoEvent{

    @Id
    private String id;

    private Long accountId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public AccountEvent(
            Long accountId,
            String action,
            Map<String, Object> details) {
        this.accountId = accountId;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
