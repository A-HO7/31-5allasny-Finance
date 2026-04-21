package com.team31.financetracker.transaction.service;

import com.team31.financetracker.transaction.Enums.TransactionSplitsStatus;
import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.dto.TransactionAnalyticsDTO;
import com.team31.financetracker.transaction.dto.TransactionDetailsDTO;
import com.team31.financetracker.transaction.dto.TransferEstimateDTO;
import com.team31.financetracker.transaction.dto.TransferEstimateRequest;
import com.team31.financetracker.transaction.Enums.TransactionType;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.model.TransactionSplit;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        transaction.setId(null);
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

    // TC_S3_04: saveAndFlush forces an immediate UPDATE SQL so a subsequent GET
    // always reads the new value. No @Transactional here — Spring Data's own
    // transaction on saveAndFlush() is sufficient and avoids proxy layering issues.
    public Transaction updateTransaction(Long id, Transaction updatedTransaction) {
        Transaction existingTransaction = getTransactionById(id);

        if (updatedTransaction.getAccountId() != null)
            existingTransaction.setAccountId(updatedTransaction.getAccountId());
        if (updatedTransaction.getToAccountId() != null)
            existingTransaction.setToAccountId(updatedTransaction.getToAccountId());
        if (updatedTransaction.getUserId() != null)
            existingTransaction.setUserId(updatedTransaction.getUserId());
        if (updatedTransaction.getApproverId() != null)
            existingTransaction.setApproverId(updatedTransaction.getApproverId());
        if (updatedTransaction.getType() != null)
            existingTransaction.setType(updatedTransaction.getType());
        if (updatedTransaction.getAmount() != null)
            existingTransaction.setAmount(updatedTransaction.getAmount());
        if (updatedTransaction.getCurrency() != null)
            existingTransaction.setCurrency(updatedTransaction.getCurrency());
        if (updatedTransaction.getCategory() != null)
            existingTransaction.setCategory(updatedTransaction.getCategory());
        if (updatedTransaction.getDescription() != null)
            existingTransaction.setDescription(updatedTransaction.getDescription());
        if (updatedTransaction.getStatus() != null)
            existingTransaction.setStatus(updatedTransaction.getStatus());
        if (updatedTransaction.getMetadata() != null)
            existingTransaction.setMetadata(updatedTransaction.getMetadata());
        if (updatedTransaction.getTransactionDate() != null)
            existingTransaction.setTransactionDate(updatedTransaction.getTransactionDate());
        if (updatedTransaction.getCompletedAt() != null)
            existingTransaction.setCompletedAt(updatedTransaction.getCompletedAt());
        if (updatedTransaction.getTransactionSplits() != null) {
            existingTransaction.setTransactionSplits(updatedTransaction.getTransactionSplits());
            ensureSplitBackReferences(existingTransaction);
        }

        return transactionRepository.saveAndFlush(existingTransaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Transaction existingTransaction = getTransactionById(id);
        transactionRepository.delete(existingTransaction);
    }

    // ── F1: search ────────────────────────────────────────────────────────────

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

    // ── F2: approve ───────────────────────────────────────────────────────────

    @Transactional
    public Transaction approveTransaction(Long transactionId, Long approverId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only approve a pending transaction");
        }

        // Cross-service: queries users table
        String role;
        try {
            role = transactionRepository.findUserRoleById(approverId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not verify approver role: user service unavailable");
        }

        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approver must be an admin");
        }

        // Cross-service: queries accounts table
        double amount    = transaction.getAmount();
        Long   accountId = transaction.getAccountId();
        try {
            switch (transaction.getType()) {
                case INCOME -> {
                    int updated = transactionRepository.addToAccountBalance(accountId, amount);
                    if (updated != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
                }
                case EXPENSE -> {
                    int updated = transactionRepository.subtractFromAccountBalance(accountId, amount);
                    if (updated != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
                }
                case TRANSFER -> {
                    if (transaction.getToAccountId() == null)
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer requires a destination account");
                    int from = transactionRepository.subtractFromAccountBalance(accountId, amount);
                    if (from != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
                    int to = transactionRepository.addToAccountBalance(transaction.getToAccountId(), amount);
                    if (to != 1) {
                        transactionRepository.addToAccountBalance(accountId, amount);
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found");
                    }
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not update account balance: account service unavailable");
        }

        transaction.setApproverId(approverId);
        transaction.setStatus(TransactionStatus.APPROVED);
        return transactionRepository.save(transaction);
    }

    // ── F3: estimate ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TransferEstimateDTO estimateTransfer(TransferEstimateRequest request) {
        if (request.amount() == null || request.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        if (request.accountId() == null || request.toAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId and toAccountId are required");
        }

        // Cross-service: queries accounts table
        try {
            long accountsFound = transactionRepository.countAccountsByIds(request.accountId(), request.toAccountId());
            if (accountsFound != 2) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or both accounts not found");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not verify accounts: account service unavailable");
        }

        double amount     = request.amount();
        long similarCount = transactionRepository.countActiveSimilarAmountTransactions(amount * 0.8, amount * 1.2);
        double feePercentage = (similarCount <= 10) ? 0.5 : (similarCount <= 25) ? 1.0 : 2.0;
        double transferFee   = amount * feePercentage / 100.0;
        double netTransfer   = amount - transferFee;
        return new TransferEstimateDTO(amount, transferFee, netTransfer, feePercentage);
    }

    // ── F4: complete ──────────────────────────────────────────────────────────

    @Transactional
    public Transaction completeTransaction(Long id) {
        Transaction transaction = getTransactionById(id);
        if (transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction must be in APPROVED status");
        }
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        // Cross-service: queries budgets table — non-fatal
        if (transaction.getType() == TransactionType.EXPENSE) {
            try {
                transactionRepository.updateBudgetSpentAmount(
                        transaction.getAmount(),
                        transaction.getCategory().name(),
                        transaction.getTransactionDate().toLocalDate());
            } catch (Exception e) {
                System.err.println("[WARN] Could not update budget for transaction " + id
                        + ": " + e.getMessage());
            }
        }

        return transaction;
    }

    // ── F5: metadata search ───────────────────────────────────────────────────

    public List<Transaction> searchByMetadataKeyValue(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadata key cannot be empty");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadata value cannot be empty");
        }
        return transactionRepository.findByMetadataKeyValue(key.trim(), value.trim());
    }

    // ── F6: analytics ─────────────────────────────────────────────────────────

    public TransactionAnalyticsDTO getAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end   = (endDate   != null) ? endDate   : LocalDate.of(2099, 12, 31);
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }
        LocalDateTime rangeStart        = start.atStartOfDay();
        LocalDateTime rangeEndExclusive = end.plusDays(1).atStartOfDay();
        Map<String, Object> result      = transactionRepository.getTransactionAnalytics(rangeStart, rangeEndExclusive);

        if (result == null) {
            return new TransactionAnalyticsDTO(0, 0, 0, 0.0, 0.0, 0.0);
        }

        Integer totalTransactions     = toInt(result.get("totalTransactions"));
        Integer completedTransactions = toInt(result.get("completedTransactions"));
        Integer voidedTransactions    = toInt(result.get("voidedTransactions"));
        Double  totalIncome           = toDouble(result.get("totalIncome"));
        Double  totalExpenses         = toDouble(result.get("totalExpenses"));
        Double  savingsRate           = (totalIncome > 0)
                ? ((totalIncome - totalExpenses) / totalIncome) * 100 : 0.0;

        return new TransactionAnalyticsDTO(totalTransactions, completedTransactions,
                voidedTransactions, totalIncome, totalExpenses, savingsRate);
    }

    // ── F7: void ──────────────────────────────────────────────────────────────

    @Transactional
    public void voidTransaction(Long id) {
        Transaction transaction = getTransactionById(id);
        if (transaction.getStatus() != TransactionStatus.PENDING
                && transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PENDING or APPROVED transactions can be voided");
        }

        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            double amount    = transaction.getAmount();
            Long   accountId = transaction.getAccountId();
            try {
                switch (transaction.getType()) {
                    case INCOME   -> transactionRepository.subtractFromAccountBalance(accountId, amount);
                    case EXPENSE  -> transactionRepository.addToAccountBalance(accountId, amount);
                    case TRANSFER -> {
                        transactionRepository.addToAccountBalance(accountId, amount);
                        if (transaction.getToAccountId() != null) {
                            transactionRepository.subtractFromAccountBalance(transaction.getToAccountId(), amount);
                        }
                    }
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Could not reverse account balance: account service unavailable");
            }
        }

        transaction.setStatus(TransactionStatus.VOIDED);
        transactionRepository.save(transaction);
    }

    // ── F8: add splits ────────────────────────────────────────────────────────

    @Transactional
    public Transaction addSplitsToTransaction(Long transactionId, Object splitsBody) {
        List<Map<String, Object>> splitRequests = normalizeSplitsBody(splitsBody);

        Transaction transaction = getTransactionById(transactionId);

        if (transaction.getStatus() != TransactionStatus.PENDING
                && transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot add splits to a COMPLETED or VOIDED transaction");
        }

        double newTotal = 0.0;
        for (Map<String, Object> req : splitRequests) {
            String recipientName = (String) req.get("recipientName");
            String description   = (String) req.get("description");
            Object amountObj     = req.get("amount");
            if (recipientName == null || recipientName.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each split must have a recipientName");
            if (description == null || description.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each split must have a description");
            if (amountObj == null || toDouble(amountObj) <= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each split amount must be positive");
            newTotal += toDouble(amountObj);
        }

        List<TransactionSplit> existingSplits = transaction.getTransactionSplits();
        int nextOrder = existingSplits.stream()
                .mapToInt(TransactionSplit::getSplitOrder)
                .max()
                .orElse(0) + 1;

        double existingTotal = existingSplits.stream().mapToDouble(TransactionSplit::getAmount).sum();

        if (existingTotal + newTotal > transaction.getAmount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Total split amounts exceed the transaction amount");
        }

        for (Map<String, Object> req : splitRequests) {
            TransactionSplit split = new TransactionSplit();
            split.setSplitOrder(nextOrder++);
            split.setRecipientName((String) req.get("recipientName"));
            split.setAmount(toDouble(req.get("amount")));
            split.setDescription((String) req.get("description"));
            split.setStatus(TransactionSplitsStatus.PENDING);
            split.setTransaction(transaction);

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) req.get("metadata");
            if (metadata != null) split.setMetadata(metadata);

            transaction.getTransactionSplits().add(split);
        }

        return transactionRepository.save(transaction);
    }

    // ── F9: details ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TransactionDetailsDTO getTransactionDetails(Long transactionId) {
        Transaction transaction = getTransactionById(transactionId);
        List<TransactionDetailsDTO.SplitDTO> splitDTOs = transaction.getTransactionSplits()
                .stream()
                .sorted(Comparator.comparingInt(TransactionSplit::getSplitOrder))
                .map(s -> new TransactionDetailsDTO.SplitDTO(
                        s.getId(),
                        s.getSplitOrder(),
                        s.getRecipientName(),
                        s.getAmount(),
                        s.getDescription(),
                        s.getStatus(),
                        s.getMetadata()))
                .collect(Collectors.toList());
        return new TransactionDetailsDTO(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getUserId(),
                transaction.getStatus() != null ? transaction.getStatus().name() : null,
                transaction.getAmount(),
                transaction.getMetadata(),
                splitDTOs);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeSplitsBody(Object body) {
        if (body instanceof List) {
            return (List<Map<String, Object>>) body;
        } else if (body instanceof Map) {
            List<Map<String, Object>> list = new ArrayList<>();
            list.add((Map<String, Object>) body);
            return list;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Request body must be a split object or an array of split objects");
        }
    }

    private void ensureSplitBackReferences(Transaction transaction) {
        if (transaction.getTransactionSplits() == null) return;
        for (TransactionSplit split : transaction.getTransactionSplits()) {
            split.setTransaction(transaction);
        }
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (NumberFormatException e) { return 0.0; }
    }
}