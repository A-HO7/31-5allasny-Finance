package com.team31.financetracker.user.dto;

import java.util.List;
import java.util.Map;

public class UserProfileDTO {
        private final Long userId;
        private final String name;
        private final String email;
        private final String phone;
        private final Map<String, Object> preferences;
        private final List<FinancialGoalDTO> financialGoals;
        private final Integer totalGoals;

        public UserProfileDTO(Long userId,
                              String name,
                              String email,
                              String phone,
                              Map<String, Object> preferences,
                              List<FinancialGoalDTO> financialGoals,
                              Integer totalGoals) {
                this.userId = userId;
                this.name = name;
                this.email = email;
                this.phone = phone;
                this.preferences = preferences;
                this.financialGoals = financialGoals;
                this.totalGoals = totalGoals;
        }

        public Long getUserId() {
                return userId;
        }

        public String getName() {
                return name;
        }

        public String getEmail() {
                return email;
        }

        public String getPhone() {
                return phone;
        }

        public Map<String, Object> getPreferences() {
                return preferences;
        }

        public List<FinancialGoalDTO> getFinancialGoals() {
                return financialGoals;
        }

        public Integer getTotalGoals() {
                return totalGoals;
        }

        public static Builder builder() {
                return new Builder();
        }

        public static class Builder {
                private Long userId;
                private String name;
                private String email;
                private String phone;
                private Map<String, Object> preferences;
                private List<FinancialGoalDTO> financialGoals;
                private Integer totalGoals;

                public Builder userId(Long userId) {
                        this.userId = userId;
                        return this;
                }

                public Builder name(String name) {
                        this.name = name;
                        return this;
                }

                public Builder email(String email) {
                        this.email = email;
                        return this;
                }

                public Builder phone(String phone) {
                        this.phone = phone;
                        return this;
                }

                public Builder preferences(Map<String, Object> preferences) {
                        this.preferences = preferences;
                        return this;
                }

                public Builder financialGoals(List<FinancialGoalDTO> financialGoals) {
                        this.financialGoals = financialGoals;
                        return this;
                }

                public Builder totalGoals(Integer totalGoals) {
                        this.totalGoals = totalGoals;
                        return this;
                }

                public UserProfileDTO build() {
                        return new UserProfileDTO(
                                userId,
                                name,
                                email,
                                phone,
                                preferences,
                                financialGoals,
                                totalGoals
                        );
                }
        }
}
