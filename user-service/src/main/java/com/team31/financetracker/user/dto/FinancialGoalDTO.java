package com.team31.financetracker.user.dto;

import java.time.LocalDate;
import java.util.Map;

public record FinancialGoalDTO(
        String label,
        Double targetAmount,
        Double currentAmount,
        LocalDate deadline,
        Boolean isPrimary,
        Map<String, Object> metadata
) {}
