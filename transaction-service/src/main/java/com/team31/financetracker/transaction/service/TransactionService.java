package com.team31.financetracker.transaction.service;

import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.Enums.TransactionType;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.model.TransactionSplit;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction createTransaction(Transaction transaction) {
        ensureSplitBackReferences(transaction);
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    public Transaction updateTransaction(Long id, Transaction updatedTransaction) {
        Transaction existingTransaction = getTransactionById(id);

        existingTransaction.setAccountId(updatedTransaction.getAccountId());
        existingTransaction.setToAccountId(updatedTransaction.getToAccountId());
        existingTransaction.setUserId(updatedTransaction.getUserId());
        existingTransaction.setApproverId(updatedTransaction.getApproverId());
        existingTransaction.setType(updatedTransaction.getType());
        existingTransaction.setAmount(updatedTransaction.getAmount());
        existingTransaction.setCurrency(updatedTransaction.getCurrency());
        existingTransaction.setCategory(updatedTransaction.getCategory());
        existingTransaction.setDescription(updatedTransaction.getDescription());
        existingTransaction.setStatus(updatedTransaction.getStatus());
        existingTransaction.setMetadata(updatedTransaction.getMetadata());
        existingTransaction.setTransactionDate(updatedTransaction.getTransactionDate());
        existingTransaction.setCompletedAt(updatedTransaction.getCompletedAt());
        existingTransaction.setTransactionSplits(updatedTransaction.getTransactionSplits());

        ensureSplitBackReferences(existingTransaction);
        return transactionRepository.save(existingTransaction);
    }

    public void deleteTransaction(Long id) {
        Transaction existingTransaction = getTransactionById(id);
        transactionRepository.delete(existingTransaction);
    }

    @Transactional
    public Transaction completeTransaction(Long id) {
        Transaction transaction = getTransactionById(id);

        if (transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction must be in APPROVED status");
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());

        if (transaction.getType() == TransactionType.EXPENSE) {
            transactionRepository.updateBudgetSpentAmount(
                    transaction.getAmount(),
                    transaction.getCategory().name(),
                    transaction.getTransactionDate().toLocalDate());
        }

        return transactionRepository.save(transaction);
    }

    private void ensureSplitBackReferences(Transaction transaction) {
        if (transaction.getTransactionSplits() == null) {
            return;
        }

        for (TransactionSplit split : transaction.getTransactionSplits()) {
            split.setTransaction(transaction);
        }
    }
}
