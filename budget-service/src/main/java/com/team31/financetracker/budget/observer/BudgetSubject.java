package com.team31.financetracker.budget.observer;

public interface BudgetSubject {
    void registerObserver(EntityObserver observer);

    void unregisterObserver(EntityObserver observer);

    void notifyObservers(String eventType, Object payload);
}