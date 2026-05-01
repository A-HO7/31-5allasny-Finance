package com.team31.financetracker.account.factory;

import com.team31.financetracker.account.mongo.AccountEvent;
import com.team31.financetracker.account.observer.MongoEvent;

import java.util.Map;

public class EventFactory {
    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {
        switch(type){
            case ACCOUNT:
                return new AccountEvent(
                        (Long) params.get("accountId"),
                        (String) params.get("action"),
                        params
                );
            case AUTH:
                throw new UnsupportedOperationException("AUTH events belong to user-service");
            case TRANSACTION:
                throw new UnsupportedOperationException("TRANSACTION events belong to transaction-service");
            case BUDGET:
                throw new UnsupportedOperationException("BUDGET events belong to budget-service");
            case REPORT_AUDIT:
                throw new UnsupportedOperationException("REPORT_AUDIT events belong to reporting-service");
            default:
                throw new IllegalArgumentException("Unknown event type: " + type);
        }
    }
}
