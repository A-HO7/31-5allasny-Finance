package com.team31.financetracker.account.service;

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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.time.LocalDateTime;
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
