package com.team31.financetracker.contracts.feign;

import com.team31.financetracker.contracts.dto.BudgetDTO;
import com.team31.financetracker.contracts.dto.BudgetSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "budget-service", url = "${feign.budget-service.url}")
public interface BudgetServiceClient {

    // S1-F4: active-budget pre-check before user deactivation
    @GetMapping("/api/budgets/user/{userId}/active-count")
    int getActiveBudgetCount(@PathVariable("userId") Long userId);

    // S3-F4: saga pre-check — find the ACTIVE budget for this user+category (404 = no budget, acceptable)
    @GetMapping("/api/budgets/user/{userId}/active")
    BudgetDTO getActiveBudgetForUser(@PathVariable("userId") Long userId,
                                     @RequestParam("category") String category);

    // S5-F10: health score budgetAdherenceRate component
    @GetMapping("/api/budgets/user/{userId}/summary")
    BudgetSummaryDTO getUserBudgetSummary(@PathVariable("userId") Long userId,
                                          @RequestParam("startDate") String startDate,
                                          @RequestParam("endDate") String endDate);

    @GetMapping("/api/budgets/{id}")
    BudgetDTO getBudget(@PathVariable("id") Long id);
}
