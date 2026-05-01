package com.team31.financetracker.user.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}