package com.team31.financetracker.user.factory;

import com.team31.financetracker.user.model.nosql.*;
import java.util.Map;

public class EventFactory {
    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {
        String action = (String) params.get("action");
        if (action == null) {
            action = type.name();
        }

        return switch (type) {
            case AUTH -> {
                Long userId = null;
                if (params.containsKey("userId") && params.get("userId") != null) {
                    userId = ((Number) params.get("userId")).longValue();
                }
                yield new AuthEvent(userId, action, params);
            }
            case ACCOUNT -> {
                Long accountId = null;
                if (params.containsKey("accountId") && params.get("accountId") != null) {
                    accountId = ((Number) params.get("accountId")).longValue();
                }
                yield new AccountEvent(accountId, action, params);
            }
            case TRANSACTION -> {
                Long transactionId = null;
                if (params.containsKey("transactionId") && params.get("transactionId") != null) {
                    transactionId = ((Number) params.get("transactionId")).longValue();
                }
                yield new TransactionEvent(transactionId, action, params);
            }
            case BUDGET -> {
                Long budgetId = null;
                if (params.containsKey("budgetId") && params.get("budgetId") != null) {
                    budgetId = ((Number) params.get("budgetId")).longValue();
                }
                yield new BudgetEvent(budgetId, action, params);
            }
            case REPORT_AUDIT -> {
                String reportType = (String) params.get("reportType");
                Integer pagesGenerated = null;
                if (params.containsKey("pagesGenerated") && params.get("pagesGenerated") != null) {
                    pagesGenerated = ((Number) params.get("pagesGenerated")).intValue();
                }
                yield new ReportAuditEvent(reportType, pagesGenerated, action, params);
            }
        };
    }
}