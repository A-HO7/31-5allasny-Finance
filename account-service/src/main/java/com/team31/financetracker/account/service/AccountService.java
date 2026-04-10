package com.team31.financetracker.account.service;

import com.team31.financetracker.account.dto.AccountStatementAlertDTO;
import com.team31.financetracker.account.dto.TopAccountDTO;
import com.team31.financetracker.account.dto.AccountSummaryDTO;
import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatement;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import com.team31.financetracker.account.repository.AccountRepository;
import com.team31.financetracker.account.repository.AccountStatementRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountStatementRepository accountStatementRepository;

    public AccountService(
            AccountRepository accountRepository,
            AccountStatementRepository accountStatementRepository
    ) {
        this.accountRepository = accountRepository;
        this.accountStatementRepository = accountStatementRepository;
    }

    public Account create(Account account) {
        return accountRepository.save(account);
    }

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
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setCurrency(updated.getCurrency());
        existing.setBalance(updated.getBalance());
        existing.setStatus(updated.getStatus());
        existing.setAccountDetails(updated.getAccountDetails());
        return accountRepository.save(existing);
    }

    public void delete(Long id) {
        accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        accountRepository.deleteById(id);
    }

    public List<Account> getByUserEmail(String email) {
        return accountRepository.findByUserEmail(email);
    }

    public int updateStatusById(Long id, AccountStatus status) {
        return accountRepository.updateStatusById(id, status.name());
    }

    @Transactional
    public void rateAccountAfterStatementReview(Long accountId, Long statementId, Integer rating) {
        if (statementId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "statementId is required");
        }
        if (rating == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating is required");
        }
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be between 1 and 5");
        }
        Account account = getById(accountId);
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
        double nextAvg = (priorAvg * prior + rating) / nextTotal;
        account.setRating(nextAvg);
        account.setTotalRatings(nextTotal);
        accountRepository.save(account);
    }
  
    @Transactional
    public void freezeAccount(Long id, AccountStatus newStatus) {
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        Account account = getById(id);
        if (newStatus == AccountStatus.FROZEN
                && accountRepository.countPendingTransactionsForAccount(id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account has pending transactions");
        }
        account.setStatus(newStatus);
        accountRepository.save(account);
    }
  
    public List<Account> searchByStatusAndBalanceRange(AccountStatus status, Double minBalance, Double maxBalance) {
        if (minBalance == null || maxBalance == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minBalance and maxBalance are required");
        }
        if (minBalance > maxBalance) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid balance range");
        }
        String statusParam = status != null ? status.name() : null;
        return accountRepository.searchByStatusAndBalanceRange(statusParam, minBalance, maxBalance);
    }
      
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
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Map<String, Object> merged = new HashMap<>();
        if (account.getAccountDetails() != null) {
            merged.putAll(account.getAccountDetails());
        }
        if (accountDetails != null) {
            merged.putAll(accountDetails);
        }
        account.setAccountDetails(merged);
        return accountRepository.save(account);
    }
      
    public List<Account> searchByDetail(String key, String value, AccountStatus status) {
        String statusValue = status == null ? null : status.name();
        return accountRepository.findByDetailKeyValueAndOptionalStatus(key, value, statusValue);
    }

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
        // Find account, throw 404 if not found
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // Find the AccountStatement by ID, throw 404 if not found
        AccountStatement statement = accountStatementRepository.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));

        // Verify it belongs to this account
        // Throws 400 if it doesn’t belong to the specified account
        if (statement.getAccount() == null || !statement.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statement does not belong to the specified account");
        }

        //  Check the statement is not expired (expiryDate is in the future)
        //  Throws 400 if expired
        if(!statement.getExpiryDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statement has expired");
        }

        // Verify the verifiedBy that is the userID being sent in the request body is an actual Admin User
        // Throws 403 if not)
        if (verifiedBy == null || !accountRepository.isAdminUser(verifiedBy)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to verify this statement");
        }

        // Update the statement’s JSONB metadata
        // To add verifiedAt timestamp and verifiedBy (from request body)
        Map<String, Object> metadata = new HashMap<>();
        if (statement.getMetadata() != null) {
            metadata.putAll(statement.getMetadata());
        }
        metadata.put("verifiedAt", LocalDateTime.now().toString());
        metadata.put("verifiedBy", verifiedBy);

        // Set verified = true
        statement.setVerified(true);
        statement.setMetadata(metadata);
        accountStatementRepository.save(statement);

        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }
      
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

