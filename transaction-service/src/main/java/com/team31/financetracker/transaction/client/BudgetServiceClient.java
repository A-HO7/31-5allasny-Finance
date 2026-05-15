package com.team31.financetracker.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * M3 (S3-READ-DB): OpenFeign client for budget-service.
 * Used as a soft dependency — never called on the hot path in S3,
 * but the interface is required by the S3-READ-DB slice contract
 * so that other slices can inject it against the contracts module.
 */
@FeignClient(name = "budget-service", url = "${feign.budget-service.url}")
public interface BudgetServiceClient {

    /**
     * Get the most recent ACTIVE budget for a user in a category.
     * Used by the saga pre-check (S3-F4) to verify a budget exists
     * before completing an EXPENSE transaction.
     * Returns 404 if no active budget exists for that category.
     */
    @GetMapping("/api/budgets/user/{userId}/active")
    Map<String, Object> getActiveBudget(
            @PathVariable("userId") Long userId,
            @RequestParam("category") String category);

    /**
     * Count of ACTIVE budgets for a user.
     * Mirrors the endpoint S4-READ-DB exposes.
     */
    @GetMapping("/api/budgets/user/{userId}/active-count")
    int getActiveBudgetCount(@PathVariable("userId") Long userId);
}
