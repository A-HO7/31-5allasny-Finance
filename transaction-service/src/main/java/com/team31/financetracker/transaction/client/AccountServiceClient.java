package com.team31.financetracker.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * M3: OpenFeign client for account-service.
 * Replaces direct SQL JOINs/UPDATEs on the accounts table (financedb-accounts).
 */
@FeignClient(name = "account-service", url = "${feign.account-service.url}")
public interface AccountServiceClient {

    /**
     * Get account owner (userId). 404 if account not found.
     * Used by S3-F11 (record-pattern).
     */
    @GetMapping("/api/accounts/{id}/owner")
    Map<String, Object> getOwner(@PathVariable("id") Long accountId);

    /**
     * Batch existence check for one or two accounts.
     * Returns {allExist: boolean}.
     * Used by S3-F3 (transfer fee estimate).
     */
    @GetMapping("/api/accounts/exists")
    Map<String, Object> accountsExist(@RequestParam("ids") String ids);
}
