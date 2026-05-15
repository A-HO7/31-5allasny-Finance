package com.team31.financetracker.contracts.feign;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "reporting-service", url = "${feign.reporting-service.url}")
public interface ReportingServiceClient {
    // Synchronous HTTP endpoints for reporting-service are added here if needed
}
