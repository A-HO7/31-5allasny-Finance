package com.team31.financetracker.contracts.feign;
import com.team31.financetracker.contracts.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "account-service", url = "${feign.account-service.url:http://account-service:8080}")
public interface AccountServiceClient {
    @GetMapping("/api/accounts/{id}")
    AccountDTO getAccount(@PathVariable("id") Long id);

    @GetMapping("/api/accounts/{id}/owner")
    OwnerDTO getOwner(@PathVariable("id") Long id);

    @GetMapping("/api/accounts/exists")
    AccountsExistDTO accountsExist(@RequestParam("ids") List<Long> ids);

    @GetMapping("/api/accounts/user/{userId}/balance-summary")
    AccountBalanceSummaryDTO getBalanceSummary(@PathVariable("userId") Long userId);
}
