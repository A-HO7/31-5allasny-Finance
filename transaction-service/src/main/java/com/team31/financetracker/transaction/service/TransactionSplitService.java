package com.team31.financetracker.transaction.service;

import com.team31.financetracker.transaction.Enums.TransactionSplitsStatus;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.model.TransactionSplit;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import com.team31.financetracker.transaction.repository.TransactionSplitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    // ═══════════════════════════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public TransactionSplit createTransactionSplit(TransactionSplit split) {
        split.setId(null); // enforce auto-generation

        // Validate amount
        if (split.getAmount() == null || split.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Amount must be positive");
        }

        // Resolve and validate the owning transaction via the repository
        Transaction transaction = resolveTransaction(split);
        split.setTransaction(transaction);

        // Auto-assign splitOrder if not provided (continue from current max for this
        // transaction)
        if (split.getSplitOrder() == null) {
            int maxOrder = transactionSplitRepository.findAll().stream()
                    .filter(s -> s.getTransaction() != null
                            && s.getTransaction().getId().equals(transaction.getId()))
                    .mapToInt(s -> s.getSplitOrder() == null ? 0 : s.getSplitOrder())
                    .max()
                    .orElse(0);
            split.setSplitOrder(maxOrder + 1);
        }

        // Default status to PENDING if not provided
        if (split.getStatus() == null) {
            split.setStatus(TransactionSplitsStatus.PENDING);
        }

        return transactionSplitRepository.save(split);
    }

    public List<TransactionSplit> getAllTransactionSplits() {
        return transactionSplitRepository.findAll();
    }

    public TransactionSplit getTransactionSplitById(Long id) {
        return transactionSplitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "TransactionSplit not found"));
    }

    /** Partial-update: only non-null fields overwrite the stored entity. */
    @Transactional
    public TransactionSplit updateTransactionSplit(Long id, TransactionSplit updatedSplit) {
        TransactionSplit existing = getTransactionSplitById(id);

        if (updatedSplit.getSplitOrder() != null)
            existing.setSplitOrder(updatedSplit.getSplitOrder());
        if (updatedSplit.getRecipientName() != null)
            existing.setRecipientName(updatedSplit.getRecipientName());
        if (updatedSplit.getAmount() != null) {
            if (updatedSplit.getAmount() <= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Amount must be positive");
            existing.setAmount(updatedSplit.getAmount());
        }
        if (updatedSplit.getDescription() != null)
            existing.setDescription(updatedSplit.getDescription());
        if (updatedSplit.getStatus() != null)
            existing.setStatus(updatedSplit.getStatus());
        if (updatedSplit.getMetadata() != null && !updatedSplit.getMetadata().isEmpty())
            existing.setMetadata(updatedSplit.getMetadata());

        // Allow re-assigning to a different transaction if caller provides one
        if (updatedSplit.getTransaction() != null
                && updatedSplit.getTransaction().getId() != null) {
            existing.setTransaction(resolveTransaction(updatedSplit));
        }

        return transactionSplitRepository.save(existing);
    }

    @Transactional
    public void deleteTransactionSplit(Long id) {
        TransactionSplit existing = getTransactionSplitById(id);
        transactionSplitRepository.delete(existing);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /** Validates and loads the Transaction the split belongs to. */
    private Transaction resolveTransaction(TransactionSplit split) {
        if (split.getTransaction() == null || split.getTransaction().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A valid transaction.id is required");
        }
        return transactionRepository.findById(split.getTransaction().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Transaction not found"));
    }
}