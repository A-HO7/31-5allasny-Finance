package com.team31.financetracker.contracts.events;
public record UserRegisteredEvent(Long userId, String email, String role) {}
