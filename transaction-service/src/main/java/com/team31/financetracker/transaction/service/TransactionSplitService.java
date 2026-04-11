package com.team31.financetracker.transaction.service;

import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.model.TransactionSplit;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import com.team31.financetracker.transaction.repository.TransactionSplitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TransactionSplitService {

    private final TransactionSplitRepository transactionSplitRepository;
    private final TransactionRepository transactionRepository;

    public TransactionSplitService(TransactionSplitRepository transactionSplitRepository,
            TransactionRepository transactionRepository) {
        this.transactionSplitRepository = transactionSplitRepository;
        this.transactionRepository = transactionRepository;
    }

    public TransactionSplit createTransactionSplit(TransactionSplit split) {
        split.setId(null);
        if (split.getAmount() != null && split.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        split.setTransaction(resolveTransaction(split));
        return transactionSplitRepository.save(split);
    }

    public List<TransactionSplit> getAllTransactionSplits() {
        return transactionSplitRepository.findAll();
    }

    public TransactionSplit getTransactionSplitById(Long id) {
        return transactionSplitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction split not found"));
    }

    public TransactionSplit updateTransactionSplit(Long id, TransactionSplit updatedSplit) {
        TransactionSplit existingSplit = getTransactionSplitById(id);

        if (updatedSplit.getSplitOrder() != null) existingSplit.setSplitOrder(updatedSplit.getSplitOrder());
        if (updatedSplit.getRecipientName() != null) existingSplit.setRecipientName(updatedSplit.getRecipientName());
        
        if (updatedSplit.getAmount() != null) {
            if (updatedSplit.getAmount() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
            }
            existingSplit.setAmount(updatedSplit.getAmount());
        }
        
        if (updatedSplit.getDescription() != null) existingSplit.setDescription(updatedSplit.getDescription());
        if (updatedSplit.getStatus() != null) existingSplit.setStatus(updatedSplit.getStatus());
        if (updatedSplit.getMetadata() != null) existingSplit.setMetadata(updatedSplit.getMetadata());

        if (updatedSplit.getTransaction() != null && updatedSplit.getTransaction().getId() != null) {
            existingSplit.setTransaction(resolveTransaction(updatedSplit));
        }

        return transactionSplitRepository.save(existingSplit);
    }

    public void deleteTransactionSplit(Long id) {
        TransactionSplit existingSplit = getTransactionSplitById(id);
        transactionSplitRepository.delete(existingSplit);
    }

    private Transaction resolveTransaction(TransactionSplit split) {
        if (split.getTransaction() == null || split.getTransaction().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transaction.id is required");
        }

        return transactionRepository.findById(split.getTransaction().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction not found"));
    }
}
