package com.team31.financetracker.contracts.feign;
import com.team31.financetracker.contracts.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "transaction-service", url = "${feign.transaction-service.url:http://transaction-service:8080}")
public interface TransactionServiceClient {
    @GetMapping("/api/transactions/user/{userId}/summary")
    UserTransactionSummaryDTO getUserTransactionSummary(@PathVariable("userId") Long userId);

    @GetMapping("/api/transactions/user/{userId}/net-income")
    NetIncomeDTO getUserNetIncome(@PathVariable("userId") Long userId, @RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate);

    @GetMapping("/api/transactions/user/{userId}/completed-count")
    long getCompletedTransactionCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/transactions/account/{accountId}/summary")
    AccountTransactionSummaryDTO getAccountTransactionSummary(
        @PathVariable("accountId") Long accountId,
        @RequestParam(value = "startDate", required = false) String startDate,
        @RequestParam(value = "endDate", required = false) String endDate
    );

    @GetMapping("/api/transactions/account/{accountId}/pending-count")
    int getPendingTransactionCount(@PathVariable("accountId") Long accountId);

    @GetMapping("/api/transactions/{transactionId}")
    TransactionDTO getTransaction(@PathVariable("transactionId") Long transactionId);
}
