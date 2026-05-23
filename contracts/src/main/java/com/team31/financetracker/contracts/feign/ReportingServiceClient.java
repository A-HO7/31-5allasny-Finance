package com.team31.financetracker.contracts.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "reporting-service", url = "${feign.reporting-service.url:http://reporting-service:8080}")
public interface ReportingServiceClient {
    
    // S5 self-health checks: Count of GENERATED reports created via the saga snapshot path
    @GetMapping("/api/reports/user/{userId}/snapshot-count")
    long getSnapshotCount(@PathVariable("userId") Long userId);
}
