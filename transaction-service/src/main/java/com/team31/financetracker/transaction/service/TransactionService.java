package com.team31.financetracker.transaction.service;

import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.Enums.TransactionType;
import com.team31.financetracker.transaction.dto.TransactionAnalyticsDTO;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.model.TransactionSplit;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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


    public List<Transaction> searchByDateRangeAndOptionalStatus(
            LocalDate startDate, LocalDate endDate, TransactionStatus status) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end   = (endDate   != null) ? endDate   : LocalDate.of(2099, 12, 31);
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }
        LocalDateTime rangeStart        = start.atStartOfDay();
        LocalDateTime rangeEndExclusive = end.plusDays(1).atStartOfDay();
        if (status != null) {
            return transactionRepository.findByStatusAndTransactionDateRange(status, rangeStart, rangeEndExclusive);
        }
        return transactionRepository.findByTransactionDateRange(rangeStart, rangeEndExclusive);
    }


    @Transactional
    public Transaction completeTransaction(Long id) {
        Transaction transaction = getTransactionById(id);
        if (transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transaction must be in APPROVED status");
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


    public List<Transaction> searchByMetadataKeyValue(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadata key cannot be empty");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadata value cannot be empty");
        }
        return transactionRepository.findByMetadataKeyValue(key.trim(), "\"" + value.trim() + "\"");
    }


    public TransactionAnalyticsDTO getAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end   = (endDate   != null) ? endDate   : LocalDate.of(2099, 12, 31);
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }
        LocalDateTime rangeStart        = start.atStartOfDay();
        LocalDateTime rangeEndExclusive = end.plusDays(1).atStartOfDay();
        Map<String, Object> result = transactionRepository.getTransactionAnalytics(rangeStart, rangeEndExclusive);
        Integer totalTransactions     = ((Number) result.get("totalTransactions")).intValue();
        Integer completedTransactions = ((Number) result.get("completedTransactions")).intValue();
        Integer voidedTransactions    = ((Number) result.get("voidedTransactions")).intValue();
        Double totalIncome            = ((Number) result.get("totalIncome")).doubleValue();
        Double totalExpenses          = ((Number) result.get("totalExpenses")).doubleValue();
        Double savingsRate = (totalIncome > 0)
                ? ((totalIncome - totalExpenses) / totalIncome) * 100
                : 0.0;
        return new TransactionAnalyticsDTO(totalTransactions, completedTransactions,
                voidedTransactions, totalIncome, totalExpenses, savingsRate);
    }


    @Transactional
    public void voidTransaction(Long id) {
        Transaction transaction = getTransactionById(id);
        if (transaction.getStatus() != TransactionStatus.PENDING &&
                transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transaction status is not voidable");
        }
        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            double delta = (transaction.getType() == TransactionType.INCOME)
                    ? -transaction.getAmount()
                    : transaction.getAmount();
            if (transaction.getType() == TransactionType.TRANSFER) {
                transactionRepository.adjustAccountBalance(transaction.getAmount(), transaction.getAccountId());
                if (transaction.getToAccountId() != null) {
                    transactionRepository.adjustAccountBalance(-transaction.getAmount(), transaction.getToAccountId());
                }
            } else {
                transactionRepository.adjustAccountBalance(delta, transaction.getAccountId());
            }
        }
        transaction.setStatus(TransactionStatus.VOIDED);
        transactionRepository.save(transaction);
    }



    @Transactional
    public Transaction addSplits(Long transactionId,
                                 List<Map<String, Object>> splitRequests) {
        Transaction transaction = transactionRepository.findByIdWithSplits(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaction not found with id: " + transactionId));

        TransactionStatus currentStatus = transaction.getStatus();
        if (currentStatus != TransactionStatus.PENDING
                && currentStatus != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot add splits to a transaction with status: " + currentStatus
                            + ". Only PENDING or APPROVED transactions accept splits.");
        }

        for (Map<String, Object> req : splitRequests) {
            String recipientName = (String) req.get("recipientName");
            String description   = (String) req.get("description");
            Object amountObj     = req.get("amount");
            if (recipientName == null || recipientName.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each split must have a non-blank recipientName.");
            if (description == null || description.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each split must have a non-blank description.");
            if (amountObj == null || toDouble(amountObj) <= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each split must have a positive amount.");
        }

        Double existingSum = transactionRepository.sumExistingSplitAmounts(transactionId);
        if (existingSum == null) existingSum = 0.0;
        double newSum = splitRequests.stream().mapToDouble(r -> toDouble(r.get("amount"))).sum();
        if (existingSum + newSum > transaction.getAmount())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Total split amounts would exceed the transaction amount.");

        int maxOrder = transactionRepository.findMaxSplitOrder(transactionId);

        List<TransactionSplit> newSplits = new ArrayList<>();
        for (int i = 0; i < splitRequests.size(); i++) {
            Map<String, Object> req = splitRequests.get(i);
            TransactionSplit split = new TransactionSplit();
            split.setSplitOrder(maxOrder + i + 1);
            split.setRecipientName((String) req.get("recipientName"));
            split.setAmount(toDouble(req.get("amount")));
            split.setDescription((String) req.get("description"));
            // status defaults to PENDING via @PrePersist on TransactionSplit
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) req.get("metadata");
            if (metadata != null) split.setMetadata(metadata);
            newSplits.add(split);
        }

        for (TransactionSplit split : newSplits) transaction.addTransactionSplit(split);
        return transactionRepository.save(transaction);
    }


    private void ensureSplitBackReferences(Transaction transaction) {
        if (transaction.getTransactionSplits() == null) return;
        for (TransactionSplit split : transaction.getTransactionSplits()) {
            split.setTransaction(transaction);
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }
}