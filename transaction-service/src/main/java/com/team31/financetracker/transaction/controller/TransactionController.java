package com.team31.financetracker.transaction.controller;

import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.dto.TransactionAnalyticsDTO;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.service.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        return transactionService.createTransaction(transaction);
    }

    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/{id}")
    public Transaction getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id);
    }

    @PutMapping("/{id}")
    public Transaction updateTransaction(@PathVariable Long id,
                                         @RequestBody Transaction transaction) {
        return transactionService.updateTransaction(id, transaction);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
    }

    @GetMapping("/search")
    public List<Transaction> searchTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return transactionService.searchByDateRangeAndOptionalStatus(startDate, endDate, status);
    }

    @PutMapping("/{id}/complete")
    public Transaction completeTransaction(@PathVariable Long id) {
        return transactionService.completeTransaction(id);
    }

    @GetMapping("/metadata/search")
    public List<Transaction> searchByMetadata(
            @RequestParam String key,
            @RequestParam String value) {
        return transactionService.searchByMetadataKeyValue(key, value);
    }

    @GetMapping("/analytics")
    public TransactionAnalyticsDTO getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return transactionService.getAnalytics(startDate, endDate);
    }

    @PutMapping("/{id}/void")
    @ResponseStatus(HttpStatus.OK)
    public void voidTransaction(@PathVariable Long id) {
        transactionService.voidTransaction(id);
    }


    @PostMapping("/{transactionId}/splits")
    public ResponseEntity<Transaction> addSplits(
            @PathVariable Long transactionId,
            @RequestBody List<Map<String, Object>> splitRequests) {
        Transaction updated = transactionService.addSplits(transactionId, splitRequests);
        return ResponseEntity.ok(updated);
    }
}