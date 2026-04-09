package com.team31.financetracker.account.controller;

import com.team31.financetracker.account.model.AccountStatement;
import com.team31.financetracker.account.model.StatementType;
import com.team31.financetracker.account.service.AccountStatementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public AccountStatement getStatementById(@PathVariable Long id) {
        return accountStatementService.getById(id);
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

    @PostMapping("/account/{accountId}")
    public AccountStatement createStatement(@PathVariable Long accountId, @RequestBody AccountStatement statement) {
        return accountStatementService.create(accountId, statement);
    }

    @PutMapping("/{id}")
    public AccountStatement updateStatement(@PathVariable Long id, @RequestBody AccountStatement statement)
    {
        return accountStatementService.update(id, statement);
    }

    @DeleteMapping("/{id}")
    public void deleteStatement(@PathVariable Long id) {
        accountStatementService.delete(id);
    }

}
