package com.team31.financetracker.user.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

public record ActivityEventDTO(
        String action,
        LocalDateTime timestamp,
        Map<String, Object> details
) implements Serializable {
}