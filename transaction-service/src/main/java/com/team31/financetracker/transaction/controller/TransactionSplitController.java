package com.team31.financetracker.transaction.controller;

import com.team31.financetracker.transaction.model.TransactionSplit;
import com.team31.financetracker.transaction.service.TransactionSplitService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Flat split CRUD controller.
 * Base path: /api/transactions/splits
 *
 * Spring MVC routes /api/transactions/splits here (literal segment "splits"
 * takes priority over the /{id} path variable in TransactionController).
 */
@RestController
@RequestMapping("/api/transactions/splits")
public class TransactionSplitController {

    private final TransactionSplitService transactionSplitService;

    public TransactionSplitController(TransactionSplitService transactionSplitService) {
        this.transactionSplitService = transactionSplitService;
    }

    /** POST /api/transactions/splits */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionSplit createTransactionSplit(@RequestBody TransactionSplit split) {
        return transactionSplitService.createTransactionSplit(split);
    }

    /** GET /api/transactions/splits */
    @GetMapping
    public List<TransactionSplit> getAllTransactionSplits() {
        return transactionSplitService.getAllTransactionSplits();
    }

    /** GET /api/transactions/splits/{id} */
    @GetMapping("/{id}")
    public TransactionSplit getTransactionSplitById(@PathVariable Long id) {
        return transactionSplitService.getTransactionSplitById(id);
    }

    /** PUT /api/transactions/splits/{id} */
    @PutMapping("/{id}")
    public TransactionSplit updateTransactionSplit(@PathVariable Long id,
                                                  @RequestBody TransactionSplit split) {
        return transactionSplitService.updateTransactionSplit(id, split);
    }

    /** DELETE /api/transactions/splits/{id} */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransactionSplit(@PathVariable Long id) {
        transactionSplitService.deleteTransactionSplit(id);
    }
}
