package com.team31.financetracker.account.controller;

import com.team31.financetracker.account.dto.RateAccountRequest;
import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import com.team31.financetracker.account.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController (AccountService accountService){
        this.accountService = accountService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("{id}")
    public Account getAccountById(@PathVariable Long id) {
        return accountService.getById(id);
    }

    @GetMapping("/user/{userId}")
    public Account getAccountByUserId(@PathVariable Long userId) {
        return accountService.getByUserId(userId);
    }

    @GetMapping("type/{type}")
    public List<Account> getAccountsByType(@PathVariable AccountType type) {
        return accountService.getByType(type);
    }

    @GetMapping("status/{status}")
    public List<Account> getAccountsByStatus(@PathVariable AccountStatus status) {
        return accountService.getByStatus(status);
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAll();
    }

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountService.create(account);
    }

    @PutMapping("{id}")
    public Account updateAccount(@PathVariable Long id, @RequestBody Account account) {
        return accountService.update(id, account);
    }

    @DeleteMapping("{id}")
    public void deleteAccount(@PathVariable Long id) {
        accountService.delete(id);
    }

    @GetMapping("/email")
    public List<Account> getByUserEmail(@RequestParam String email) {
        return accountService.getByUserEmail(email);
    }

    @PutMapping("/{id}/status")
    public int updateStatus(@PathVariable Long id, @RequestParam AccountStatus status) {
        return accountService.updateStatusById(id, status);
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Void> rateAccount(@PathVariable Long id, @RequestBody RateAccountRequest body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        accountService.rateAccountAfterStatementReview(id, body.getStatementId(), body.getRating());
        return ResponseEntity.ok().build();
    }

}
