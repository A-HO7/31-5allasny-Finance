package com.team31.financetracker.contracts.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Mirrors the fields exposed by user-service's GET /api/users/{id} endpoint.
 * Fields match the User entity: id, name, email, role, status, preferences.
 */
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;
    private String role;
    private String status;
    private Map<String, Object> preferences;

    public UserDTO() {}

    public UserDTO(Long id, String name, String email, String role, String status,
                   Map<String, Object> preferences) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.preferences = preferences;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> preferences) { this.preferences = preferences; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String email;
        private String role;
        private String status;
        private Map<String, Object> preferences;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder preferences(Map<String, Object> preferences) { this.preferences = preferences; return this; }

        public UserDTO build() {
            return new UserDTO(id, name, email, role, status, preferences);
        }
    }
}
