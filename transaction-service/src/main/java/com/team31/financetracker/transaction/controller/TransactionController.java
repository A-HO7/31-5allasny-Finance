package com.team31.financetracker.transaction.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.dto.TransactionAnalyticsDashboardDTO;
import com.team31.financetracker.transaction.dto.TransactionAnalyticsDTO;
import com.team31.financetracker.transaction.dto.TransactionDetailsDTO;
import com.team31.financetracker.transaction.dto.TransferEstimateDTO;
import com.team31.financetracker.transaction.dto.TransferEstimateRequest;
import com.team31.financetracker.transaction.dto.CategoryRecommendationDTO;
import com.team31.financetracker.transaction.security.AuthContext;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.model.TransactionSplit;
import com.team31.financetracker.transaction.service.TransactionService;
import com.team31.financetracker.transaction.service.TransactionSplitService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Primary controller for the Transaction Service.
 * Base path: /api/transactions
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionSplitService transactionSplitService;
    private final ObjectMapper objectMapper;
    private final AuthContext authContext;

    public TransactionController(TransactionService transactionService,
            TransactionSplitService transactionSplitService,
            ObjectMapper objectMapper,
            AuthContext authContext) {
        this.transactionService = transactionService;
        this.transactionSplitService = transactionSplitService;
        this.objectMapper = objectMapper;
        this.authContext = authContext;
    }

    // ── Transaction CRUD ──────────────────────────────────────────────────────

    /** POST /api/transactions */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        if (transaction.getAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId is required");
        }
        if (transaction.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (transaction.getAmount() == null || transaction.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive");
        }
        if (transaction.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
        }
        return transactionService.createTransaction(transaction);
    }

    /** GET /api/transactions */
    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    /** GET /api/transactions/{id} */
    @GetMapping("/{id}")
    public Transaction getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id);
    }

    /** PUT /api/transactions/{id} */
    @PutMapping("/{id}")
    public Transaction updateTransaction(@PathVariable Long id,
            @RequestBody Transaction transaction) {
        return transactionService.updateTransaction(id, transaction);
    }

    /** DELETE /api/transactions/{id} */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
    }

    // ── F1: Search ────────────────────────────────────────────────────────────

    /** GET /api/transactions/search?status=&startDate=&endDate= */
    @GetMapping("/search")
    public List<Transaction> searchTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return transactionService.searchByDateRangeAndOptionalStatus(startDate, endDate, status);
    }

    // ── F2: Approve ───────────────────────────────────────────────────────────

    /** PUT /api/transactions/{transactionId}/approve?approverId={userId} */
    @PutMapping("/{transactionId}/approve")
    public Transaction approveTransaction(@PathVariable Long transactionId,
            @RequestParam Long approverId) {
        return transactionService.approveTransaction(transactionId, approverId);
    }

    // ── F3: Transfer Fee Estimate ─────────────────────────────────────────────

    /** POST /api/transactions/estimate */
    @PostMapping("/estimate")
    public TransferEstimateDTO estimateTransfer(@RequestBody TransferEstimateRequest request) {
        return transactionService.estimateTransfer(request);
    }

    // ── F4: Complete ──────────────────────────────────────────────────────────

    /** PUT /api/transactions/{id}/complete */
    @PutMapping("/{id}/complete")
    public Transaction completeTransaction(@PathVariable Long id) {
        return transactionService.completeTransaction(id);
    }

    // ── F5: Metadata Search ───────────────────────────────────────────────────

    /** GET /api/transactions/metadata/search?key=&value= */
    @GetMapping("/metadata/search")
    public List<Transaction> searchByMetadata(@RequestParam String key,
            @RequestParam String value) {
        return transactionService.searchByMetadataKeyValue(key, value);
    }

    // ── F6: Analytics ─────────────────────────────────────────────────────────

    /** GET /api/transactions/analytics?startDate=&endDate= */
    @GetMapping("/analytics")
    public TransactionAnalyticsDTO getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return transactionService.getAnalytics(startDate, endDate);
    }

    /** GET /api/transactions/analytics/dashboard?startDate=&endDate= */
    @GetMapping("/analytics/dashboard")
    public TransactionAnalyticsDashboardDTO getAnalyticsDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        transactionService.logAnalyticsViewedEvent(startDate, endDate);
        return transactionService.getDashboardAnalytics(startDate, endDate);
    }

    // ── F7: Void ──────────────────────────────────────────────────────────────

    /** PUT /api/transactions/{id}/void */
    @PutMapping("/{id}/void")
    @ResponseStatus(HttpStatus.OK)
    public void voidTransaction(@PathVariable Long id) {
        transactionService.voidTransaction(id);
    }

    // ── F8: Add Splits (single object OR array) ───────────────────────────────

    /**
     * POST /api/transactions/{transactionId}/splits
     *
     * Accepts EITHER:
     * a single split object { "recipientName": "...", "amount": 50.0, ... }
     * OR an array [ { ... }, { ... } ]
     *
     * We read the raw body as a JsonNode, detect which shape it is, normalise
     * to List<TransactionSplit>, then delegate to the service.
     *
     * The service signature is:
     * Transaction addSplitsToTransaction(Long transactionId, List<TransactionSplit>
     * splits)
     */
    @PostMapping("/{transactionId}/splits")
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction addSplitsToTransaction(@PathVariable Long transactionId,
            @RequestBody JsonNode splitsBody) {
        List<TransactionSplit> splits;
        if (splitsBody.isArray()) {
            splits = objectMapper.convertValue(
                    splitsBody, new TypeReference<List<TransactionSplit>>() {
                    });
        } else {
            TransactionSplit single = objectMapper.convertValue(
                    splitsBody, TransactionSplit.class);
            splits = List.of(single);
        }

        if (splits == null || splits.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "splits list must not be empty");
        }
        for (TransactionSplit split : splits) {
            if (split.getRecipientName() == null || split.getRecipientName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each split must have a recipientName");
            }
            if (split.getAmount() == null || split.getAmount() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each split amount must be positive");
            }
        }
        return transactionService.addSplitsToTransaction(transactionId, splits);
    }

    // ── Nested split CRUD ─────────────────────────────────────────────────────

    /** GET /api/transactions/{transactionId}/splits/{splitId} */
    @GetMapping("/{transactionId}/splits/{splitId}")
    public TransactionSplit getSplitById(@PathVariable Long transactionId,
            @PathVariable Long splitId) {
        TransactionSplit split = transactionSplitService.getTransactionSplitById(splitId);
        validateSplitOwnership(split, transactionId);
        return split;
    }

    /** PUT /api/transactions/{transactionId}/splits/{splitId} */
    @PutMapping("/{transactionId}/splits/{splitId}")
    public TransactionSplit updateSplit(@PathVariable Long transactionId,
            @PathVariable Long splitId,
            @RequestBody TransactionSplit split) {
        TransactionSplit existing = transactionSplitService.getTransactionSplitById(splitId);
        validateSplitOwnership(existing, transactionId);
        return transactionSplitService.updateTransactionSplit(splitId, split);
    }

    /** DELETE /api/transactions/{transactionId}/splits/{splitId} */
    @DeleteMapping("/{transactionId}/splits/{splitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSplit(@PathVariable Long transactionId,
            @PathVariable Long splitId) {
        TransactionSplit existing = transactionSplitService.getTransactionSplitById(splitId);
        validateSplitOwnership(existing, transactionId);
        transactionSplitService.deleteTransactionSplit(splitId);
    }

    // ── F9: Transaction Details with Splits ───────────────────────────────────

    /** GET /api/transactions/{transactionId}/details */
    @GetMapping("/{transactionId}/details")
    public TransactionDetailsDTO getTransactionDetails(@PathVariable Long transactionId) {
        return transactionService.getTransactionDetails(transactionId);
    }

    // ── S3-F11: Record User-Category Spending Pattern ────────────────────────

    /** POST /api/transactions/{transactionId}/record-pattern */
    @PostMapping("/{transactionId}/record-pattern")
    @ResponseStatus(HttpStatus.OK)
    public void recordSpendingPattern(@PathVariable Long transactionId) {
        transactionService.recordSpendingPattern(transactionId);
    }

    // ── S3-F12: Get Category Recommendations for User ───────────────────────

    /**
     * GET
     * /api/transactions/recommendations?userId={id}&limit={n}&categoryType={type}
     *
     * Ownership rule: a non-ADMIN caller may only request recommendations for
     * their own userId. ADMIN may request recommendations for any userId.
     */
    @GetMapping("/recommendations")
    public List<CategoryRecommendationDTO> getCategoryRecommendations(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String categoryType) {
        // Ownership check — ADMIN bypasses, others must match their own userId
        String callerRole = authContext.getRole();
        Long callerUserId = authContext.getUserId();
        if (!"ADMIN".equals(callerRole) && !userId.equals(callerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: you can only view your own recommendations");
        }
        return transactionService.getCategoryRecommendations(userId, limit, categoryType);
    }

    // ── M3 Internal endpoints (called by other services via Feign) ───────────

    /** GET /api/transactions/user/{userId}/summary */
    @GetMapping("/user/{userId}/summary")
    public Map<String, Object> getUserTransactionSummary(@PathVariable Long userId) {
        Map<String, Object> raw = transactionService.getUserTransactionSummary(userId);
        Map<String, Object> result = new HashMap<>(raw);
        return result;
    }

    /** GET /api/transactions/user/{userId}/net-income?startDate=&endDate= */
    @GetMapping("/user/{userId}/net-income")
    public Map<String, Object> getUserNetIncome(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end   = (endDate   != null) ? endDate   : LocalDate.of(2099, 12, 31);
        LocalDateTime rangeStart        = start.atStartOfDay();
        LocalDateTime rangeEndExclusive = end.plusDays(1).atStartOfDay();
        return new HashMap<>(transactionService.getUserNetIncome(userId, rangeStart, rangeEndExclusive));
    }

    /** GET /api/transactions/user/{userId}/completed-count */
    @GetMapping("/user/{userId}/completed-count")
    public long getUserCompletedCount(@PathVariable Long userId) {
        return transactionService.countCompletedTransactionsByUserId(userId);
    }

    /** GET /api/transactions/account/{accountId}/summary?startDate=&endDate= */
    @GetMapping("/account/{accountId}/summary")
    public Map<String, Object> getAccountTransactionSummary(
            @PathVariable Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return new HashMap<>(transactionService.getAccountTransactionSummaryAllTime(accountId));
        }
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end   = (endDate   != null) ? endDate   : LocalDate.of(2099, 12, 31);
        LocalDateTime rangeStart        = start.atStartOfDay();
        LocalDateTime rangeEndExclusive = end.plusDays(1).atStartOfDay();
        return new HashMap<>(transactionService.getAccountTransactionSummary(accountId, rangeStart, rangeEndExclusive));
    }

    /** GET /api/transactions/account/{accountId}/pending-count */
    @GetMapping("/account/{accountId}/pending-count")
    public long getAccountPendingCount(@PathVariable Long accountId) {
        return transactionService.countPendingTransactionsByAccountId(accountId);
    }

    // ── Private helper ────────────────────────────────────────────────────────

    private void validateSplitOwnership(TransactionSplit split, Long transactionId) {
        if (split.getTransaction() == null
                || !split.getTransaction().getId().equals(transactionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Split not found for this transaction");
        }
    }
}