package com.team31.financetracker.account.event;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
