package com.team31.financetracker.budget.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}