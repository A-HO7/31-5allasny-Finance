package com.team31.financetracker.transaction.dto;

public record CategoryRecommendationDTO(
        String category,
        String categoryType,
        Integer score,
        Double averageAmount
) {}