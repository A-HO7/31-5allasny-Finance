package com.team31.financetracker.account.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
