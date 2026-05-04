package com.team31.financetracker.account.service;

import com.team31.financetracker.account.dto.AccountStatementAlertDTO;
import com.team31.financetracker.account.dto.TopAccountDTO;
import com.team31.financetracker.account.dto.AccountSummaryDTO;
import com.team31.financetracker.account.observer.EntityObserver;
import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatement;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import com.team31.financetracker.account.repository.AccountRepository;
import com.team31.financetracker.account.repository.AccountStatementRepository;
import com.team31.financetracker.account.util.CacheInvalidator;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountStatementRepository accountStatementRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final CacheInvalidator cacheInvalidator;
    public AccountService(
            AccountRepository accountRepository,
            AccountStatementRepository accountStatementRepository,
            CacheInvalidator cacheInvalidator, CacheInvalidator cacheInvalidator1
    ) {
        this.accountRepository = accountRepository;
        this.accountStatementRepository = accountStatementRepository;
        this.cacheInvalidator = cacheInvalidator1;
    }

    public void register(EntityObserver observer){
        observers.add(observer);
    }

    public void unregister(EntityObserver observer){
        observers.remove(observer);
    }

    public void notifyObservers(String eventType, Object payload){
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    public Account create(Account account) {
        applyCreateDefaults(account);
        return accountRepository.save(account);
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
        cacheInvalidator.invalidateAccountData(id); // Clear stale cache
        return saved;
    }

    public void delete(Long id) {
        accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        accountRepository.deleteById(id);
        cacheInvalidator.invalidateAccountData(id);
    }

    public List<Account> getByUserEmail(String email) {
        return accountRepository.findByUserEmail(email);
    }

    public int updateStatusById(Long id, AccountStatus status) {
        int rowsAffected =  accountRepository.updateStatusById(id, status.name());
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
        accountRepository.save(account);
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
        accountRepository.saveAndFlush(account);
        cacheInvalidator.invalidateAccountData(id);
    }

    @Cacheable(value = "account-service::S2-F9", key = "'expired-statements'")
    public List<AccountStatementAlertDTO> getAccountsWithExpiredStatements() {
        List<Account> accounts = accountRepository.findAccountsWithExpiredStatementsNative();

        return accounts.stream().map(account -> {
                    List<AccountStatement> expired = account.getAccountStatements().stream()
                            .filter(s -> s.getExpiryDate().isBefore(LocalDate.now()))
                            .toList();

                    return new AccountStatementAlertDTO(
                            account.getId(),
                            account.getName(),
                            account.getStatus().name(),
                            expired,
                            expired.size()
                    );
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
        return results.stream().map(row -> new TopAccountDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).doubleValue(),
                ((Number) row[3]).longValue()
        )).toList();
    }

    @Transactional
    public Account verifyStatement(Long accountId, Long statementId, Long verifiedBy) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        AccountStatement statement = accountStatementRepository.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));

        if (statement.getAccount() == null || !statement.getAccount().getId().equals(accountId)) {
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

    @Cacheable(value = "account-service::S2-F3", key = "#id + '-' + #start + '-' + #end")
    public AccountSummaryDTO getSummary(Long id, LocalDateTime start, LocalDateTime end) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Object resultRaw = accountRepository.getAccountSummaryNative(id, start, end);

        if (resultRaw == null) {
            return new AccountSummaryDTO(id, account.getName(), 0.0, 0.0, 0.0, 0L);
        }

        Object[] row = (Object[]) resultRaw;

        Long accountId = ((Number) row[0]).longValue();
        String name = (String) row[1];
        Double totalDeposits = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        Double totalWithdrawals = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        Long transactionCount = ((Number) row[4]).longValue();

        Double netChange = totalDeposits - totalWithdrawals;

        return new AccountSummaryDTO(accountId, name, totalDeposits, totalWithdrawals, netChange, transactionCount);
    }
}
