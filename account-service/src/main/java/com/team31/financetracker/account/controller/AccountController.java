package com.team31.financetracker.account.controller;

import com.team31.financetracker.account.dto.AccountStatementAlertDTO;
import com.team31.financetracker.account.dto.AccountSummaryDTO;
import com.team31.financetracker.account.dto.FreezeAccountRequest;
import com.team31.financetracker.account.dto.RateAccountRequest;
import com.team31.financetracker.account.dto.RequestDTO;
import com.team31.financetracker.account.dto.TopAccountDTO;
import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import com.team31.financetracker.account.service.AccountService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/search")
    public List<Account> searchAccounts(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) Double minBalance,
            @RequestParam(required = false) Double maxBalance) {
        return accountService.searchByStatusAndBalanceRange(status, minBalance, maxBalance);
    }

    @GetMapping("/statements/expired")
    public List<AccountStatementAlertDTO> getAccountsWithExpiredStatements() {
        return accountService.getAccountsWithExpiredStatements();
    }

    @GetMapping("/reports/top-balance")
    public List<TopAccountDTO> getTopBalanceAccounts(@RequestParam int limit) {
        return accountService.getTopBalanceAccounts(limit);
    }

    @GetMapping("/details/search")
    public List<Account> searchByDetail(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) AccountStatus status
    ) {
        return accountService.searchByDetail(key, value, status);
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAll();
    }

    @GetMapping("/email")
    public List<Account> getByUserEmail(@RequestParam String email) {
        return accountService.getByUserEmail(email);
    }

    @GetMapping("/user/{userId}")
    public List<Account> getAccountByUserId(@PathVariable Long userId) {
        return accountService.getByUserId(userId);
    }

    @GetMapping("/type/{type}")
    public List<Account> getAccountsByType(@PathVariable AccountType type) {
        return accountService.getByType(type);
    }

    @GetMapping("/status/{status}")
    public List<Account> getAccountsByStatus(@PathVariable AccountStatus status) {
        return accountService.getByStatus(status);
    }

    @GetMapping("/{id}/summary")
    public AccountSummaryDTO getSummary(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return accountService.getSummary(id, startDate, endDate);
    }

    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable Long id) {
        return accountService.getById(id);
    }

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountService.create(account);
    }

    @PutMapping("/{id}")
    public Account updateAccount(@PathVariable Long id, @RequestBody Account account) {
        return accountService.update(id, account);
    }

    @PutMapping("/{id}/details")
    public Account updateAccountDetails(
            @PathVariable Long id,
            @RequestBody Map<String, Object> details
    ) {
        return accountService.updateAccountDetails(id, details);
    }

    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id) {
        accountService.delete(id);
    }

    @PutMapping("/{id}/status")
    public int updateStatus(@PathVariable Long id, @RequestParam AccountStatus status) {
        return accountService.updateStatusById(id, status);
    }

    @PutMapping("/{id}/freeze")
    public ResponseEntity<Void> freezeAccount(@PathVariable Long id, @RequestBody FreezeAccountRequest body) {
        if (body == null || body.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body with status is required");
        }
        accountService.freezeAccount(id, body.getStatus());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Void> rateAccount(@PathVariable Long id, @RequestBody RateAccountRequest body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        accountService.rateAccountAfterStatementReview(id, body.getStatementId(), body.getRating());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{accountId}/statements/{statementId}/verify")
    public Account verifyStatement(
            @PathVariable Long accountId,
            @PathVariable Long statementId,
            @RequestBody RequestDTO request
    ) {
        return accountService.verifyStatement(accountId, statementId, request.getVerifiedBy());
    }
}
