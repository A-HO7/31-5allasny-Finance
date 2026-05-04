package com.team31.financetracker.transaction.service;

import com.team31.financetracker.transaction.Enums.TransactionSplitsStatus;
import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.Enums.TransactionType;
import com.team31.financetracker.transaction.dto.TransactionAnalyticsDTO;
import com.team31.financetracker.transaction.dto.TransactionDetailsDTO;
import com.team31.financetracker.transaction.dto.TransferEstimateDTO;
import com.team31.financetracker.transaction.dto.TransferEstimateRequest;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.model.TransactionSplit;
import com.team31.financetracker.transaction.neo4j.SpentOnRelationship;
import com.team31.financetracker.transaction.observer.EntityObserver;
import com.team31.financetracker.transaction.observer.MongoEventLogger;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team31.financetracker.transaction.repository.CategoryNodeRepository;
import com.team31.financetracker.transaction.repository.UserNodeRepository;
import com.team31.financetracker.transaction.util.TransactionAnalyticsAdapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    // Service for managing transactions and spending patterns.


    private final TransactionRepository transactionRepository;
    private final UserNodeRepository userNodeRepository;
    private final CategoryNodeRepository categoryNodeRepository;
    private final CacheInvalidationService cacheInvalidationService;

    // ── Observer Pattern (DP-2) ───────────────────────────────────────────────
    // Each service owns its own observer list — not shared across services.
    private final List<EntityObserver> observers = new ArrayList<>();

    /**
     * MongoEventLogger is injected by Spring and registered immediately so that
     * every write endpoint fires events from the first request onward.
     */
    public TransactionService(TransactionRepository transactionRepository,
            UserNodeRepository userNodeRepository,
            CategoryNodeRepository categoryNodeRepository,
            MongoEventLogger mongoEventLogger,
            CacheInvalidationService cacheInvalidationService) {
        this.transactionRepository = transactionRepository;
        this.userNodeRepository = userNodeRepository;
        this.categoryNodeRepository = categoryNodeRepository;
        this.cacheInvalidationService = cacheInvalidationService;
        registerObserver(mongoEventLogger);
    }

    /** Register an observer to receive state-change notifications. */
    public void registerObserver(EntityObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /** Unregister an observer (used in unit tests to verify the observer path). */
    public void unregisterObserver(EntityObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify all registered observers of a state change.
     * Called AFTER the PostgreSQL save so the observer can read the persisted
     * state.
     * Any exception thrown by an observer is caught inside MongoEventLogger — it
     * never propagates here and never rolls back the PG transaction.
     */
    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        transaction.setId(null); // enforce auto-generation
        ensureSplitBackReferences(transaction); // fix back-refs if splits were embedded
        Transaction saved = transactionRepository.save(transaction);
        notifyObservers("TRANSACTION_CREATED", saved); // DP-2 Observer
        cacheInvalidationService.evictAllTransactionCaches(saved.getId());
        return saved;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Cacheable(value = "transaction-service", key = "'transaction::' + #id")
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    /**
     * Partial-update: only non-null fields from the request body overwrite the
     * stored entity.
     */
    @Transactional
    public Transaction updateTransaction(Long id, Transaction updatedTransaction) {
        Transaction existing = getTransactionById(id);

        if (updatedTransaction.getAccountId() != null)
            existing.setAccountId(updatedTransaction.getAccountId());
        if (updatedTransaction.getToAccountId() != null)
            existing.setToAccountId(updatedTransaction.getToAccountId());
        if (updatedTransaction.getUserId() != null)
            existing.setUserId(updatedTransaction.getUserId());
        if (updatedTransaction.getApproverId() != null)
            existing.setApproverId(updatedTransaction.getApproverId());
        if (updatedTransaction.getType() != null)
            existing.setType(updatedTransaction.getType());
        if (updatedTransaction.getAmount() != null)
            existing.setAmount(updatedTransaction.getAmount());
        if (updatedTransaction.getCurrency() != null)
            existing.setCurrency(updatedTransaction.getCurrency());
        if (updatedTransaction.getCategory() != null)
            existing.setCategory(updatedTransaction.getCategory());
        if (updatedTransaction.getDescription() != null)
            existing.setDescription(updatedTransaction.getDescription());
        if (updatedTransaction.getStatus() != null)
            existing.setStatus(updatedTransaction.getStatus());
        if (updatedTransaction.getTransactionDate() != null)
            existing.setTransactionDate(updatedTransaction.getTransactionDate());
        if (updatedTransaction.getCompletedAt() != null)
            existing.setCompletedAt(updatedTransaction.getCompletedAt());
        // metadata: only overwrite when the incoming map is explicitly non-null AND
        // non-empty
        if (updatedTransaction.getMetadata() != null && !updatedTransaction.getMetadata().isEmpty())
            existing.setMetadata(updatedTransaction.getMetadata());

        Transaction saved = transactionRepository.saveAndFlush(existing);
        notifyObservers("TRANSACTION_UPDATED", saved); // DP-2 Observer
        cacheInvalidationService.evictAllTransactionCaches(saved.getId());
        return saved;
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Transaction existing = getTransactionById(id);
        // Fire event BEFORE delete so the payload still carries the entity's fields
        notifyObservers("TRANSACTION_DELETED", existing); // DP-2 Observer
        transactionRepository.delete(existing);
        cacheInvalidationService.evictAllTransactionCaches(id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // F1 — Get Transactions by Status and Date Range
    // ═══════════════════════════════════════════════════════════════════════════

    @Cacheable(value = "transaction-service", key = "'S3-F1::' + #startDate + '-' + #endDate + '-' + #status")
    public List<Transaction> searchByDateRangeAndOptionalStatus(
            LocalDate startDate, LocalDate endDate, TransactionStatus status) {

        // Default to full range when not provided
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.of(2099, 12, 31);

        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must not be after endDate");
        }

        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEndExclusive = end.plusDays(1).atStartOfDay();

        if (status != null) {
            return transactionRepository
                    .findByStatusAndTransactionDateRange(status, rangeStart, rangeEndExclusive);
        }
        return transactionRepository.findByTransactionDateRange(rangeStart, rangeEndExclusive);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // F2 — Approve Transaction (Transactional) [M1 write → Observer]
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public Transaction approveTransaction(Long transactionId, Long approverId) {
        Transaction transaction = getTransactionById(transactionId);

        // Validate status
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PENDING transactions can be approved");
        }

        // Verify approver exists and is ADMIN (cross-service: users table)
        String role;
        try {
            role = transactionRepository.findUserRoleById(approverId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "User not found"));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not verify approver role");
        }

        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Approver must have the ADMIN role");
        }

        // Update account balance (cross-service: accounts table)
        double amount = transaction.getAmount();
        Long accountId = transaction.getAccountId();

        try {
            switch (transaction.getType()) {
                case INCOME -> {
                    int updated = transactionRepository.addToAccountBalance(accountId, amount);
                    if (updated != 1)
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
                }
                case EXPENSE -> {
                    int updated = transactionRepository.subtractFromAccountBalance(accountId, amount);
                    if (updated != 1)
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
                }
                case TRANSFER -> {
                    if (transaction.getToAccountId() == null)
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "TRANSFER requires a toAccountId");
                    int from = transactionRepository.subtractFromAccountBalance(accountId, amount);
                    if (from != 1)
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Source account not found");
                    int to = transactionRepository.addToAccountBalance(
                            transaction.getToAccountId(), amount);
                    if (to != 1) {
                        // roll back the deduct
                        transactionRepository.addToAccountBalance(accountId, amount);
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Destination account not found");
                    }
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not update account balance");
        }

        transaction.setApproverId(approverId);
        transaction.setStatus(TransactionStatus.APPROVED);
        Transaction saved = transactionRepository.save(transaction);
        notifyObservers("APPROVED", saved); // DP-2 Observer — S3-F2 write
        cacheInvalidationService.evictAllTransactionCaches(saved.getId());
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // F3 — Get Transfer Fee Estimate (DTO, read-only — no observer needed)
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    @Cacheable(value = "transaction-service", key = "'S3-F3::' + #request.hashCode()")
    public TransferEstimateDTO estimateTransfer(TransferEstimateRequest request) {
        // Validate amount
        if (request.amount() == null || request.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "amount must be positive");
        }
        if (request.accountId() == null || request.toAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "accountId and toAccountId are required");
        }

        // Validate both accounts exist (cross-service)
        try {
            long found = transactionRepository.countAccountsByIds(
                    request.accountId(), request.toAccountId());
            if (found != 2) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "One or both accounts not found");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not verify accounts");
        }

        // Determine fee tier based on similar active transaction count
        double amount = request.amount();
        long similar = transactionRepository.countActiveSimilarAmountTransactions(
                amount * 0.8, amount * 1.2);

        double feePercentage = (similar <= 10) ? 0.5 : (similar <= 25) ? 1.0 : 2.0;
        double transferFee = amount * feePercentage / 100.0;
        double netTransfer = amount - transferFee;

        return TransferEstimateDTO.builder()
                .amount(amount)
                .transferFee(transferFee)
                .netTransfer(netTransfer)
                .feePercentage(feePercentage)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // F4 — Complete Transaction (Transactional) [M1 write → Observer]
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public Transaction completeTransaction(Long id) {
        Transaction transaction = getTransactionById(id);

        if (transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only APPROVED transactions can be completed");
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        // Update matching budget's spentAmount (cross-service, non-fatal)
        if (transaction.getType() == TransactionType.EXPENSE) {
            try {
                transactionRepository.updateBudgetSpentAmount(
                        transaction.getAmount(),
                        transaction.getCategory().name(),
                        transaction.getTransactionDate().toLocalDate());
            } catch (Exception e) {
                System.err.println("[WARN] Could not update budget for transaction "
                        + id + ": " + e.getMessage());
            }
        }

        notifyObservers("COMPLETED", transaction); // DP-2 Observer — S3-F4 write
        cacheInvalidationService.evictAllTransactionCaches(transaction.getId());
        return transaction;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // F5 — Filter Transactions by Metadata Field (JSONB, read-only)
    // ═══════════════════════════════════════════════════════════════════════════

    @Cacheable(value = "transaction-service", key = "'S3-F5::' + #key + '-' + #value")
    public List<Transaction> searchByMetadataKeyValue(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Metadata key must not be empty");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Metadata value must not be empty");
        }
        return transactionRepository.findByMetadataKeyValue(key.trim(), value.trim());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // F6 — Transaction Analytics by Time Period (read-only)
    // ═══════════════════════════════════════════════════════════════════════════

    @Cacheable(value = "transaction-service", key = "'S3-F6::' + #startDate + '-' + #endDate")
    public TransactionAnalyticsDTO getAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.of(2099, 12, 31);

        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must not be after endDate");
        }

        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEndExclusive = end.plusDays(1).atStartOfDay();

        Map<String, Object> raw = transactionRepository
                .getTransactionAnalytics(rangeStart, rangeEndExclusive);

        TransactionAnalyticsAdapter adapter = new TransactionAnalyticsAdapter();
        return adapter.adapt(raw);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // F7 — Void Transaction (Transactional) [M1 write → Observer]
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void voidTransaction(Long id) {
        Transaction transaction = getTransactionById(id);

        if (transaction.getStatus() != TransactionStatus.PENDING
                && transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PENDING or APPROVED transactions can be voided");
        }

        // If APPROVED, the balance was already modified during approval — reverse it
        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            double amount = transaction.getAmount();
            Long accountId = transaction.getAccountId();
            try {
                switch (transaction.getType()) {
                    case INCOME -> transactionRepository.subtractFromAccountBalance(accountId, amount);
                    case EXPENSE -> transactionRepository.addToAccountBalance(accountId, amount);
                    case TRANSFER -> {
                        transactionRepository.addToAccountBalance(accountId, amount);
                        if (transaction.getToAccountId() != null) {
                            transactionRepository.subtractFromAccountBalance(
                                    transaction.getToAccountId(), amount);
                        }
                    }
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Could not reverse account balance");
            }
        }

        transaction.setStatus(TransactionStatus.VOIDED);
        Transaction saved = transactionRepository.save(transaction);
        notifyObservers("VOIDED", saved); // DP-2 Observer — S3-F7 write
        cacheInvalidationService.evictAllTransactionCaches(saved.getId());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // F8 — Add Splits to Transaction (Finance Tracker deviation write → Observer)
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public Transaction addSplitsToTransaction(Long transactionId,
            List<TransactionSplit> splitRequests) {
        Transaction transaction = getTransactionById(transactionId);

        // Validate transaction status
        if (transaction.getStatus() != TransactionStatus.PENDING
                && transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot add splits to a COMPLETED or VOIDED transaction");
        }

        // Validate each incoming split
        for (TransactionSplit req : splitRequests) {
            if (req.getRecipientName() == null || req.getRecipientName().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each split must have a recipientName");
            if (req.getDescription() == null || req.getDescription().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each split must have a description");
            if (req.getAmount() == null || req.getAmount() <= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each split amount must be positive");
        }

        // Determine starting splitOrder (continue from existing max)
        List<TransactionSplit> existingSplits = transaction.getTransactionSplits();
        int nextOrder = existingSplits.stream()
                .mapToInt(TransactionSplit::getSplitOrder)
                .max()
                .orElse(0) + 1;

        // Validate total amounts do not exceed transaction amount
        double existingTotal = existingSplits.stream()
                .mapToDouble(TransactionSplit::getAmount)
                .sum();
        double newTotal = splitRequests.stream()
                .mapToDouble(TransactionSplit::getAmount)
                .sum();

        if (existingTotal + newTotal > transaction.getAmount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Total split amounts exceed the transaction amount");
        }

        // Create and attach TransactionSplit entities via the JPA relationship
        for (TransactionSplit req : splitRequests) {
            TransactionSplit split = new TransactionSplit();
            split.setSplitOrder(nextOrder++);
            split.setRecipientName(req.getRecipientName());
            split.setAmount(req.getAmount());
            split.setDescription(req.getDescription());
            split.setStatus(TransactionSplitsStatus.PENDING);
            if (req.getMetadata() != null)
                split.setMetadata(req.getMetadata());

            transaction.addTransactionSplit(split); // sets back-reference
        }

        // Save transaction — cascade persists the new splits
        Transaction saved = transactionRepository.save(transaction);
        notifyObservers("SPLITS_ADDED", saved); // DP-2 Observer — S3-F8 Finance Tracker deviation
        cacheInvalidationService.evictAllTransactionCaches(saved.getId());
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // F9 — Get Transaction Details with Splits (DTO, read-only)
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    @Cacheable(value = "transaction-service", key = "'S3-F9::' + #transactionId")
    public TransactionDetailsDTO getTransactionDetails(Long transactionId) {
        Transaction transaction = getTransactionById(transactionId);

        List<TransactionDetailsDTO.SplitDTO> splitDTOs = transaction.getTransactionSplits()
                .stream()
                .sorted(Comparator.comparingInt(TransactionSplit::getSplitOrder))
                .map(s -> TransactionDetailsDTO.SplitDTO.builder()
                        .id(s.getId())
                        .splitOrder(s.getSplitOrder())
                        .recipientName(s.getRecipientName())
                        .amount(s.getAmount())
                        .description(s.getDescription())
                        .status(s.getStatus())
                        .metadata(s.getMetadata())
                        .build())
                .collect(Collectors.toList());

        return TransactionDetailsDTO.builder()
                .transactionId(transaction.getId())
                .accountId(transaction.getAccountId())
                .userId(transaction.getUserId())
                .status(transaction.getStatus() != null ? transaction.getStatus().name() : null)
                .amount(transaction.getAmount())
                .metadata(transaction.getMetadata())
                .splits(splitDTOs)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // S3-F11 — Record User-Category Spending Pattern (Neo4j write → Observer)
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void recordSpendingPattern(Long transactionId) {
        // a) Find the transaction
        Transaction transaction = getTransactionById(transactionId);

        // b) Verify status is COMPLETED
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only COMPLETED transactions can record spending patterns");
        }

        // c) Skip TRANSFER transactions
        if (transaction.getType() == TransactionType.TRANSFER) {
            return; // 200 OK, no-op
        }

        // d) Get userId via accounts join
        Long userId = transactionRepository.findUserIdByTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Could not find user for transaction"));

        // e) Get user details
        Map<String, Object> userDetails = transactionRepository.findUserDetailsById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Could not find user details"));

        String name = (String) userDetails.get("name");
        String currency = (String) userDetails.get("currency");

        // f) Determine categoryType
        String categoryType = (transaction.getType() == TransactionType.INCOME) ?
                "INCOME_CATEGORY" : "EXPENSE_CATEGORY";

        // g) Record in Neo4j with idempotency (Soft dependency)
        SpentOnRelationship relationship = null;
        try {
            relationship = userNodeRepository.recordSpendingPattern(
                userId, name, currency,
                transaction.getCategory().name(),
                categoryType,
                transactionId,
                transaction.getAmount(),
                transaction.getCompletedAt() != null ? transaction.getCompletedAt() : LocalDateTime.now()
            );
        } catch (Exception ex) {
            // Section 6.3: Soft dependency — log and swallow
            System.err.println("[WARN] Neo4j recordSpendingPattern failed: " + ex.getMessage());
        }

        // h) Log event only if graph was mutated (relationship != null)
        if (relationship != null) {
            Map<String, Object> eventDetails = Map.of(
                    "transactionId", transactionId,
                    "userId", userId,
                    "category", transaction.getCategory().name(),
                    "amount", transaction.getAmount()
            );
            notifyObservers("PATTERN_RECORDED", eventDetails); // DP-2 Observer — S3-F11 write
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Ensure any splits embedded in the request body have their back-reference set.
     */
    private void ensureSplitBackReferences(Transaction transaction) {
        if (transaction.getTransactionSplits() == null)
            return;
        for (TransactionSplit split : transaction.getTransactionSplits()) {
            split.setTransaction(transaction);
        }
    }

    // Removed private helpers toInt and toDouble as they are now in the adapter
}