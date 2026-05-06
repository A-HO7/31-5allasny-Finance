package com.team31.financetracker.user.dto;

import java.io.Serializable;

public record RegisterRequest(
        String name,
        String email,
        String password,
        String phone
) implements Serializable {
    private static final long serialVersionUID = 1L;
}