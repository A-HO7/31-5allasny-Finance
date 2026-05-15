package com.team31.financetracker.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * M3: OpenFeign client for user-service.
 * Replaces direct SQL JOINs on the users table (financedb-users).
 */
@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {

    /**
     * Retrieve full user info including role.
     * Used by S3-F2 (approve) to verify the approver is ADMIN.
     * Used by S3-F11 (record-pattern) and S3-F12 (recommendations).
     */
    @GetMapping("/api/users/{id}")
    Map<String, Object> getUser(@PathVariable("id") Long id);
}
