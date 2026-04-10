package com.team31.financetracker.transaction.controller;

import com.team31.financetracker.transaction.model.TransactionSplit;
import com.team31.financetracker.transaction.service.TransactionSplitService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transaction-splits")
public class TransactionSplitController {

    private final TransactionSplitService transactionSplitService;

    public TransactionSplitController(TransactionSplitService transactionSplitService) {
        this.transactionSplitService = transactionSplitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionSplit createTransactionSplit(@RequestBody TransactionSplit split) {
        return transactionSplitService.createTransactionSplit(split);
    }

    @GetMapping
    public List<TransactionSplit> getAllTransactionSplits() {
        return transactionSplitService.getAllTransactionSplits();
    }

    @GetMapping("/{id}")
    public TransactionSplit getTransactionSplitById(@PathVariable Long id) {
        return transactionSplitService.getTransactionSplitById(id);
    }

    @PutMapping("/{id}")
    public TransactionSplit updateTransactionSplit(@PathVariable Long id, @RequestBody TransactionSplit split) {
        return transactionSplitService.updateTransactionSplit(id, split);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransactionSplit(@PathVariable Long id) {
        transactionSplitService.deleteTransactionSplit(id);
    }
}
