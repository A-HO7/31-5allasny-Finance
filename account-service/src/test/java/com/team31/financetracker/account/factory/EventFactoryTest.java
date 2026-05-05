package com.team31.financetracker.account.factory;

import com.team31.financetracker.account.mongo.AccountEvent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventFactoryTest {

    @Test
    void createEvent_account_returnsAccountEventWithMatchingFields() {
        Map<String, Object> params = new HashMap<>();
        params.put("accountId", 42L);
        params.put("action", "CREATED");
        params.put("source", "unit-test");

        var event = EventFactory.createEvent(EventType.ACCOUNT, params);

        assertInstanceOf(AccountEvent.class, event);

        AccountEvent accountEvent = (AccountEvent) event;
        assertEquals(42L, accountEvent.getAccountId());
        assertEquals("CREATED", accountEvent.getAction());
        assertEquals(params, accountEvent.getDetails());
        assertNotNull(accountEvent.getTimestamp());
    }
}