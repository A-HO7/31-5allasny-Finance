package com.team31.financetracker.transaction.dto;

public record CategoryRecommendationDTO(
        String category,
        String categoryType,
        Integer score,
        Double averageAmount
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String category;
        private String categoryType;
        private Integer score;
        private Double averageAmount;

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder categoryType(String categoryType) {
            this.categoryType = categoryType;
            return this;
        }

        public Builder score(Integer score) {
            this.score = score;
            return this;
        }

        public Builder averageAmount(Double averageAmount) {
            this.averageAmount = averageAmount;
            return this;
        }

        public CategoryRecommendationDTO build() {
            return new CategoryRecommendationDTO(category, categoryType, score, averageAmount);
        }
    }
}