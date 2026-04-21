package com.team31.financetracker.account.controller;

import com.team31.financetracker.account.model.AccountStatement;
import com.team31.financetracker.account.model.StatementType;
import com.team31.financetracker.account.service.AccountStatementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/statements")
public class AccountStatementController {
    private final AccountStatementService accountStatementService;

    public AccountStatementController(AccountStatementService accountStatementService) {
        this.accountStatementService = accountStatementService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping
    public List<AccountStatement> getAllStatements() {
        return accountStatementService.getAll();
    }

    @GetMapping("/{accountId}/statements")public List<AccountStatement> getStatementsByAccount(@PathVariable Long accountId) {

        return accountStatementService.getByUserId(accountId);
    }

    @GetMapping("/type/{type}")
    public List<AccountStatement> getStatementsByType(@PathVariable StatementType type) {
        return accountStatementService.getByType(type);
    }

    @GetMapping("/before/{date}")
    public List<AccountStatement> getStatementsExpiringBefore(@PathVariable java.time.LocalDate date) {
        return accountStatementService.getExpiringBefore(date);
    }

    @GetMapping("/after/{date}")
    public List<AccountStatement> getStatementsExpiringAfter(@PathVariable java.time.LocalDate date) {
        return accountStatementService.getExpiringAfter(date);
    }

    @PostMapping("/{accountId}/statements")
    public AccountStatement createStatement(
            @PathVariable Long accountId,
            @RequestBody AccountStatement statement) {
        return accountStatementService.create(accountId, statement);
    }

    @PutMapping("/{id}")
    public AccountStatement updateStatement(@PathVariable Long id, @RequestBody AccountStatement statement)
    {
        return accountStatementService.update(id, statement);
    }

    @DeleteMapping("/{accountId}/statements/{stmtId}")
    public void deleteStatement(@PathVariable Long accountId, @PathVariable Long stmtId) {
        accountStatementService.delete(stmtId);
    }

    @GetMapping("/user/{userId}")
    public List<AccountStatement> getByUserId(@PathVariable Long userId) {
        return accountStatementService.getByUserId(userId);
    }

    @DeleteMapping("/expired")
    public int deleteExpiredBefore(@RequestParam java.time.LocalDate cutoffDate) {
        return accountStatementService.deleteExpiredBefore(cutoffDate);
    }
}
