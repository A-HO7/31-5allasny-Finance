package com.team31.financetracker.user.dto;

public record RegisterRequest(
        String name,
        String email,
        String password,
        String phone
) {}