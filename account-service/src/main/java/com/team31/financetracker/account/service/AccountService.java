package com.team31.financetracker.account.service;

import com.team31.financetracker.account.adapter.AccountSummaryProjectionAdapter;
import com.team31.financetracker.account.adapter.TopAccountProjectionAdapter;
import com.team31.financetracker.account.dto.*;
import com.team31.financetracker.account.mongo.AccountEventActions;
import com.team31.financetracker.account.model.*;
import com.team31.financetracker.account.observer.EntityObserver;
import com.team31.financetracker.account.repository.AccountRepository;
import com.team31.financetracker.account.repository.AccountSearchRepository;
import com.team31.financetracker.account.repository.AccountStatementRepository;
import com.team31.financetracker.account.util.CacheInvalidator;
import com.team31.financetracker.contracts.dto.OwnerDTO;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.team31.financetracker.account.model.AccountSearchDocument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountStatementRepository accountStatementRepository;
    private final AccountSearchRepository accountSearchRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final CacheInvalidator cacheInvalidator;
    private final AccountSummaryProjectionAdapter accountSummaryProjectionAdapter;
    private final TopAccountProjectionAdapter topAccountProjectionAdapter;
    private final RedisTemplate<String, Object> redisTemplate;

    public AccountService(
            AccountRepository accountRepository,
            AccountStatementRepository accountStatementRepository,
            AccountSearchRepository accountSearchRepository,
            CacheInvalidator cacheInvalidator,
            AccountSummaryProjectionAdapter accountSummaryProjectionAdapter,
            TopAccountProjectionAdapter topAccountProjectionAdapter,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.accountRepository = accountRepository;
        this.accountStatementRepository = accountStatementRepository;
        this.accountSearchRepository = accountSearchRepository;
        this.cacheInvalidator = cacheInvalidator;
        this.accountSummaryProjectionAdapter = accountSummaryProjectionAdapter;
        this.topAccountProjectionAdapter = topAccountProjectionAdapter;
        this.redisTemplate = redisTemplate;
    }

    public void register(EntityObserver observer){
        observers.add(observer);
    }

    public void unregister(EntityObserver observer){
        observers.remove(observer);
    }

    public void notifyObservers(String eventType, Object payload){
        for (EntityObserver observer : observers) {
            try {
                observer.onEvent(eventType, payload);
            } catch (Exception e) {
                log.warn("Observer notification failed for event [{}]: {}", eventType, e.getMessage());
            }
        }
    }

    public Account create(Account account) {
        applyCreateDefaults(account);
        Account saved = accountRepository.save(account);
        indexAccount(saved);
        notifyAccountEvent(AccountEventActions.ACCOUNT_CREATED, saved, Map.of("name", saved.getName()));
        return saved;
    }

    private static void applyCreateDefaults(Account account) {
        if (account.getStatus() == null) {
            account.setStatus(AccountStatus.ACTIVE);
        }
        if (account.getBalance() == null) {
            account.setBalance(0.0);
        }
        if (account.getRating() == null) {
            account.setRating(0.0);
        }
        if (account.getTotalRatings() == null) {
            account.setTotalRatings(0);
        }
        if (account.getCurrency() == null || account.getCurrency().isBlank()) {
            account.setCurrency("EGP");
        }
    }

    @Cacheable(value = "account-service::account", key = "#id")
    public Account getById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    public List<Account> getByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public List<Account> getByType(AccountType type) {
        return accountRepository.findByType(type);
    }

    public List<Account> getByStatus(AccountStatus status) {
        return accountRepository.findByStatus(status);
    }

    public List<Account> getAll() {
        return accountRepository.findAll();
    }

    public Account update(Long id, Account updated) {
        Account existing = getById(id);
        if (updated.getName() != null) {
            existing.setName(updated.getName());
        }
        if (updated.getType() != null) {
            existing.setType(updated.getType());
        }
        if (updated.getCurrency() != null) {
            existing.setCurrency(updated.getCurrency());
        }
        if (updated.getBalance() != null) {
            existing.setBalance(updated.getBalance());
        }
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        if (updated.getRating() != null) {
            existing.setRating(updated.getRating());
        }
        if (updated.getTotalRatings() != null) {
            existing.setTotalRatings(updated.getTotalRatings());
        }
        if (updated.getAccountDetails() != null) {
            existing.setAccountDetails(updated.getAccountDetails());
        }
        Account saved = accountRepository.save(existing);
        indexAccount(saved);
        notifyAccountEvent(AccountEventActions.ACCOUNT_UPDATED, saved, Map.of("name", saved.getName()));
        cacheInvalidator.invalidateAccountData(id); // Clear stale cache
        return saved;
    }

    public void delete(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        accountRepository.deleteById(id);
        deleteAccountIndex(id);
        notifyAccountEvent(AccountEventActions.ACCOUNT_DELETED, account, Map.of("name", account.getName()));
        cacheInvalidator.invalidateAccountData(id);
    }

    public List<Account> getByUserEmail(String email) {
        return accountRepository.findByUserEmail(email);
    }

    public int updateStatusById(Long id, AccountStatus status) {
        int rowsAffected =  accountRepository.updateStatusById(id, status.name());
        accountRepository.findById(id).ifPresent(account -> {
            indexAccount(account);
            notifyAccountEvent(AccountEventActions.ACCOUNT_UPDATED, account, Map.of("status", status.name()));
        });
        cacheInvalidator.invalidateAccountData(id);
        return rowsAffected;
    }
    @Cacheable(value = "account-service::S2-F1", key = "T(java.util.Objects).hash(#status, #minBalance, #maxBalance)")
    public List<Account> searchByStatusAndBalanceRange(AccountStatus status, Double minBalance, Double maxBalance) {
        // Return all if no balance provided (TC_S2_69)
        if (minBalance == null || maxBalance == null) {
            return (status != null) ? accountRepository.findByStatus(status) : accountRepository.findAll();
        }

        if (minBalance > maxBalance) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid range"); // 400 requirement
        }

        return accountRepository.searchByStatusAndBalanceRange(status != null ? status.name() : null, minBalance, maxBalance);

    }

    @Transactional
    public void rateAccountAfterStatementReview(Long accountId, Long statementId, Double rating) {
        Account account = getById(accountId);
        if (statementId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "statementId is required");
        }
        if (rating == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating is required");
        }
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be between 1 and 5");
        }
        if (Math.abs(rating - Math.rint(rating)) > 1e-6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be a whole number between 1 and 5");
        }
        int ratingInt = (int) Math.rint(rating);
        AccountStatement statement = accountStatementRepository.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));
        if (statement.getAccount() == null || !statement.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statement does not belong to this account");
        }
        if (!statement.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statement must be verified");
        }
        int prior = account.getTotalRatings() != null ? account.getTotalRatings() : 0;
        double priorAvg = account.getRating() != null ? account.getRating() : 0.0;
        int nextTotal = prior + 1;
        double nextAvg = (priorAvg * prior + ratingInt) / nextTotal;
        account.setRating(nextAvg);
        account.setTotalRatings(nextTotal);
        Account saved = accountRepository.save(account);
        indexAccount(saved);
        notifyAccountEvent(AccountEventActions.RATED, saved, Map.of(
                "statementId", statementId,
                "rating", ratingInt,
                "newAverage", nextAvg,
                "totalRatings", nextTotal
        ));
        cacheInvalidator.invalidateAccountData(accountId);
    }

    @Transactional
    public void freezeAccount(Long id, AccountStatus newStatus) {
        // 1. Fetch
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // 2. Validate
        if (newStatus == null) {
            newStatus = AccountStatus.FROZEN;
        }

        // 3. Logic for Frozen
        if (newStatus == AccountStatus.FROZEN) {
            // Use the count query
            long pendingCount = accountRepository.countPendingTransactionsForAccount(id);
            if (pendingCount > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account has pending transactions");
            }
        }

        // 4. Update
        account.setStatus(newStatus);

        // 5. Force Push to DB
        Account saved = accountRepository.saveAndFlush(account);
        indexAccount(saved);
        AccountEventActions action = newStatus == AccountStatus.FROZEN
                ? AccountEventActions.FROZEN
                : newStatus == AccountStatus.ACTIVE ? AccountEventActions.UNFROZEN : AccountEventActions.ACCOUNT_UPDATED;
        notifyAccountEvent(action, saved, Map.of("status", saved.getStatus().name()));
        cacheInvalidator.invalidateAccountData(id);
    }

    @Cacheable(value = "account-service::S2-F9", key = "'expired-statements'")
    public List<AccountStatementAlertDTO> getAccountsWithExpiredStatements() {
        List<Account> accounts = accountRepository.findAccountsWithExpiredStatementsNative();

        return accounts.stream().map(account -> {
                    List<AccountStatement> expired = account.getAccountStatements().stream()
                            .filter(s -> s.getExpiryDate().isBefore(LocalDate.now()))
                            .toList();

                    return AccountStatementAlertDTO.builder()
                            .accountId(account.getId())
                            .accountName(account.getName())
                            .accountStatus(account.getStatus().name())
                            .expiredStatements(expired)
                            .expiredCount(expired.size())
                            .build();
                })
                .filter(dto -> dto.expiredCount() > 0)
                .toList();
    }

    public Account updateAccountDetails(Long id, Map<String, Object> accountDetails) {
        Account account = getById(id); // Use getById to ensure 404 (TC_S2_23)

        Map<String, Object> existing = account.getAccountDetails();
        if (existing == null) existing = new HashMap<>();

        if (accountDetails != null) {
            existing.putAll(accountDetails); // Merge
        }

        account.setAccountDetails(existing);
        Account saved = accountRepository.save(account);
        indexAccount(saved);
        notifyAccountEvent(AccountEventActions.DETAILS_UPDATED, saved, Map.of("updatedKeys", existing.keySet()));
        cacheInvalidator.invalidateAccountData(id);
        return saved;
    }

    @Cacheable(value = "account-service::S2-F5", key = "#key + '-' + #value + '-' + #status")
    public List<Account> searchByDetail(String key, String value, AccountStatus status) {
        String statusValue = status == null ? null : status.name();
        return accountRepository.findByDetailKeyValueAndOptionalStatus(key, value, statusValue);
    }

    @Cacheable(value = "account-service::S2-F6", key = "#limit")
    public List<TopAccountDTO> getTopBalanceAccounts(int limit) {
        List<Object[]> results = accountRepository.getTopBalanceAccountsNative(limit);
        return results.stream()
                .map(topAccountProjectionAdapter::adapt)
                .toList();
    }

    @Transactional
    public Account verifyStatement(Long accountId, Long statementId, Long verifiedBy) {
        getById(accountId);

        AccountStatement statement = accountStatementRepository.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));

        if (statement.getAccount()
                == null || !statement.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statement does not belong to the specified account");
        }

        if (!statement.getExpiryDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statement has expired");
        }

        if (verifiedBy == null || verifiedBy <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid verifier ID");
        }
        String role = accountRepository.getUserRoleNative(verifiedBy);
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to verify this statement");
        }

        Map<String, Object> metadata = new HashMap<>();
        if (statement.getMetadata() != null) {
            metadata.putAll(statement.getMetadata());
        }
        metadata.put("verifiedAt", LocalDateTime.now().toString());
        metadata.put("verifiedBy", verifiedBy);

        statement.setVerified(true);
        statement.setMetadata(metadata);
        accountStatementRepository.saveAndFlush(statement);


        // Re-fetch with statements eagerly loaded
        Account result = accountRepository.findByIdWithStatements(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // Force initialize inside transaction so Jackson can serialize it
        result.getAccountStatements().size();
        cacheInvalidator.invalidateAccountData(accountId);
        return result;
    }
    @Cacheable(value = "account-service::S2-F10", key = "T(java.util.Objects).hash(#query, #type, #status, #currency, #min, #max, #minRating, #maxRating)")
    public List<AccountDTO> fullTextSearch(String query, String type, String status,
                                           String currency, Double min, Double max,
                                           Double minRating, Double maxRating) {
        if (min != null && max != null && min > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid balance range");
        }
        if (minRating != null && maxRating != null && minRating > maxRating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rating range");
        }

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();

        return accountRepository.findAll().stream()
                .filter(account -> matchesText(account, normalizedQuery))
                .filter(account -> type == null || account.getType().name().equalsIgnoreCase(type))
                .filter(account -> status == null || account.getStatus().name().equalsIgnoreCase(status))
                .filter(account -> currency == null || account.getCurrency().equalsIgnoreCase(currency))
                .filter(account -> min == null || account.getBalance() >= min)
                .filter(account -> max == null || account.getBalance() <= max)
                .filter(account -> minRating == null || account.getRating() >= minRating)
                .filter(account -> maxRating == null || account.getRating() <= maxRating)
                .sorted(Comparator
                        .comparingInt((Account account) -> relevanceScore(account, normalizedQuery)).reversed()
                        .thenComparing(Account::getRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Account::getId))
                .map(this::toDto)
                .toList();
    }

    private boolean matchesText(Account account, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return containsIgnoreCase(account.getName(), query)
                || containsIgnoreCase(readDescription(account), query);
    }

    private int relevanceScore(Account account, String query) {
        if (query == null || query.isBlank()) {
            return 0;
        }
        if (containsIgnoreCase(account.getName(), query)) {
            return 2;
        }
        if (containsIgnoreCase(readDescription(account), query)) {
            return 1;
        }
        return 0;
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private AccountDTO toDto(Account account) {
        return AccountDTO.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .description(readDescription(account))
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(account.getStatus())
                .rating(account.getRating())
                .build();
    }

    @Cacheable(value = "account-service::S2-F3", key = "#id + '-' + #start + '-' + #end")
    public AccountSummaryDTO getSummary(Long id, LocalDateTime start, LocalDateTime end) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Object resultRaw = accountRepository.getAccountSummaryNative(id, start, end);

        if (resultRaw == null) {
            return AccountSummaryDTO.builder()
                    .accountId(id)
                    .name(account.getName())
                    .totalDeposits(0.0)
                    .totalWithdrawals(0.0)
                    .netChange(0.0)
                    .totalTransactions(0L)
                    .build();
        }

        Object[] row = (Object[]) resultRaw;
        return accountSummaryProjectionAdapter.adapt(row);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    public AccountPerformanceDashboardDTO getDashboard(
            Long accountId,
            Long requestingUserId,
            String requestingUserRole
    ) {
        Account account = getById(accountId);

        boolean isAdmin = "ADMIN".equals(requestingUserRole);
        boolean isOwner =
                requestingUserId != null
                &&
                requestingUserId.equals(account.getUserId());

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", accountId);
        payload.put("requestedBy", requestingUserId);
        payload.put("requestedByRole", requestingUserRole);
        notifyObservers(AccountEventActions.DASHBOARD_VIEWED.name(), payload);

        return applicationContext.getBean(AccountService.class).getDashboardCached(accountId, account.getName(), account.getType().name(), account.getBalance());
    }

    @Cacheable(value = "account-service::S2-F12", key = "#accountId")
    public AccountPerformanceDashboardDTO getDashboardCached(Long accountId, String name, String type, Double balance) {
        Object[] stats = accountRepository.getTransactionStats(accountId);
        if (stats.length == 1 && stats[0] instanceof Object[] row) {
            stats = row;
        }
        Long activeStatements = accountRepository.getActiveStatementsCount(accountId);

        Long totalTransactions = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
        Double totalIncome = stats[1] != null ? ((Number) stats[1]).doubleValue() : 0.0;
        Double totalExpenses = stats[2] != null ? ((Number) stats[2]).doubleValue() : 0.0;

        return AccountPerformanceDashboardDTO.builder()
                .accountId(accountId)
                .name(name)
                .type(type)
                .balance(balance)
                .totalTransactions(totalTransactions)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netChange(totalIncome - totalExpenses)
                .activeStatementsCount(activeStatements)
                .build();
    }

    @Transactional
    public void indexAccountById(Long id) {
        Account account = getById(id);
        indexAccount(account);
    }

    private void indexAccount(Account account) {
        try {
            accountSearchRepository.save(toSearchDocument(account));
            notifyAccountEvent(AccountEventActions.INDEXED, account, Map.of("index", "accounts"));
        } catch (Exception e) {
            log.warn("Account indexing failed for account [{}]: {}", account.getId(), e.getMessage());
        }
    }

    private void deleteAccountIndex(Long id) {
        try {
            accountSearchRepository.deleteById(id.toString());
        } catch (Exception e) {
            log.warn("Account index delete failed for account [{}]: {}", id, e.getMessage());
        }
    }

    private AccountSearchDocument toSearchDocument(Account account) {
        return AccountSearchDocument.builder()
                .id(account.getId().toString())
                .name(account.getName())
                .type(account.getType() != null ? account.getType().name() : null)
                .description(readDescription(account))
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(account.getStatus() != null ? account.getStatus().name() : null)
                .rating(account.getRating())
                .build();
    }

    private String readDescription(Account account) {
        Map<String, Object> details = account.getAccountDetails();
        if (details == null) {
            return null;
        }
        Object description = details.get("description");
        if (description == null) {
            description = details.get("bankName");
        }
        return description != null ? description.toString() : null;
    }

    private void notifyAccountEvent(AccountEventActions action, Account account, Map<String, Object> details) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", account.getId());
        payload.put("action", action.name());
        payload.put("name", account.getName());
        if (details != null) {
            payload.putAll(details);
        }
        notifyObservers(action.name(), payload);
    }

    @Cacheable(value = "account-service::getAccount", key = "#id")
    public com.team31.financetracker.contracts.dto.AccountDTO getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        return com.team31.financetracker.contracts.dto.AccountDTO.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .name(account.getName())
                .type(account.getType() != null ? account.getType().name() : null)
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(account.getStatus() != null ? account.getStatus().name() : null)
                .rating(account.getRating())
                .accountDetails(account.getAccountDetails())
                .build();
    }

    public boolean doAllAccountsExist(List<Long> ids) {
        long foundCount = accountRepository.countByIdIn(ids);
        boolean allExist = foundCount == ids.size();
        return allExist;
    }
    public OwnerDTO getAccountOwner(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        return new OwnerDTO(account.getUserId());
    }

}
