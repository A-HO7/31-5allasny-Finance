package com.team31.financetracker.account.event;

import com.team31.financetracker.account.repository.AccountEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {
    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    private final AccountEventRepository eventRepository;

    public MongoEventLogger(AccountEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("action", eventType);

            if (payload instanceof Map) {
                params.putAll((Map<String, Object>) payload);
            }

            MongoEvent event = EventFactory.createEvent(EventType.ACCOUNT, params);
            eventRepository.save((AccountEvent) event);
        } catch (Exception e) {
            log.warn("MongoDB event logging failed: {}", e.getMessage());
        }
    }
}
