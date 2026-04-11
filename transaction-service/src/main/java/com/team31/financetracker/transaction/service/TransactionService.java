package com.team31.financetracker.transaction.service;

import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.dto.TransactionAnalyticsDTO;
import com.team31.financetracker.transaction.Enums.TransactionType;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.model.TransactionSplit;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    public List<Transaction> searchByDateRangeAndOptionalStatus(
            LocalDate startDate, LocalDate endDate, TransactionStatus status) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }
        var rangeStart = startDate.atStartOfDay();
        var rangeEndExclusive = endDate.plusDays(1).atStartOfDay();
        if (status != null) {
            return transactionRepository.findByStatusAndTransactionDateRange(status, rangeStart, rangeEndExclusive);
        }
        return transactionRepository.findByTransactionDateRange(rangeStart, rangeEndExclusive);
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

    public TransactionAnalyticsDTO getAnalytics(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }

        var rangeStart = startDate.atStartOfDay();
        var rangeEndExclusive = endDate.plusDays(1).atStartOfDay();

        Map<String, Object> result = transactionRepository.getTransactionAnalytics(rangeStart, rangeEndExclusive);

        Integer totalTransactions = ((Number) result.get("totalTransactions")).intValue();
        Integer completedTransactions = ((Number) result.get("completedTransactions")).intValue();
        Integer voidedTransactions = ((Number) result.get("voidedTransactions")).intValue();
        Double totalIncome = ((Number) result.get("totalIncome")).doubleValue();
        Double totalExpenses = ((Number) result.get("totalExpenses")).doubleValue();

        Double savingsRate = 0.0;
        if (totalIncome > 0) {
            savingsRate = ((totalIncome - totalExpenses) / totalIncome) * 100;
        }

        return new TransactionAnalyticsDTO(totalTransactions, completedTransactions, voidedTransactions,
                totalIncome, totalExpenses, savingsRate);
    }
    
    public List<Transaction> searchByMetadataKeyValue(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadata key cannot be empty");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadata value cannot be empty");
        }
        return transactionRepository.findByMetadataKeyValue(key.trim(), "\"" + value.trim() + "\"");
    }

    @Transactional
    public Transaction approveTransaction(Long transactionId, Long approverId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only approve a pending transaction");
        }

        String role = transactionRepository.findUserRoleById(approverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approver must be an admin");
        }

        double amount = transaction.getAmount();
        Long accountId = transaction.getAccountId();
        switch (transaction.getType()) {
            case INCOME -> {
                int updated = transactionRepository.addToAccountBalance(accountId, amount);
                if (updated != 1) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
                }
            }
            case EXPENSE -> {
                int updated = transactionRepository.subtractFromAccountBalance(accountId, amount);
                if (updated != 1) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
                }
            }
            case TRANSFER -> {
                if (transaction.getToAccountId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer requires a destination account");
                }
                int from = transactionRepository.subtractFromAccountBalance(accountId, amount);
                if (from != 1) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
                }
                int to = transactionRepository.addToAccountBalance(transaction.getToAccountId(), amount);
                if (to != 1) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found");
                }
            }
        }

        transaction.setApproverId(approverId);
        transaction.setStatus(TransactionStatus.APPROVED);
        return transactionRepository.save(transaction);
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
