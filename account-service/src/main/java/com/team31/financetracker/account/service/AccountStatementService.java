package com.team31.financetracker.account.service;

import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatement;
import com.team31.financetracker.account.model.StatementType;
import com.team31.financetracker.account.repository.AccountRepository;
import com.team31.financetracker.account.repository.AccountStatementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccountStatementService {
    private final AccountStatementRepository accountStatementRepository;
    private final AccountRepository accountRepository;

    public AccountStatementService (
            AccountRepository accountRepository,
            AccountStatementRepository accountStatementRepository
    ) {
        this.accountRepository = accountRepository;
        this.accountStatementRepository = accountStatementRepository;
    }

    public AccountStatement create(Long accountId, AccountStatement statement) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        statement.setAccount(account);
        return accountStatementRepository.save(statement);
    }

    public AccountStatement getById(Long id) {
        return accountStatementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));
    }

    public List<AccountStatement> getAll() {
        return accountStatementRepository.findAll();
    }

    public List<AccountStatement> getByType(StatementType type) {
        return accountStatementRepository.findByType(type);
    }

    public List<AccountStatement> getExpiringBefore(java.time.LocalDate date) {
        return accountStatementRepository.findByExpiryDateBefore(date);
    }

    public List<AccountStatement> getExpiringAfter(java.time.LocalDate date) {
        return accountStatementRepository.findByExpiryDateAfter(date);
    }

    public AccountStatement update(Long id, AccountStatement updated) {
        AccountStatement existing = getById(id);
        existing.setType(updated.getType());
        existing.setDocumentUrl(updated.getDocumentUrl());
        existing.setExpiryDate(updated.getExpiryDate());
        existing.setVerified(updated.isVerified());
        existing.setMetadata(updated.getMetadata());
        return accountStatementRepository.save(existing);
    }

    public void delete(Long id) {
        accountStatementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));
        accountStatementRepository.deleteById(id);
    }

    public List<AccountStatement> getByUserId(Long userId) {
        return accountStatementRepository.findByUserId(userId);
    }

    public int deleteExpiredBefore(java.time.LocalDate cutoffDate) {
        return accountStatementRepository.deleteExpiredBefore(cutoffDate);
    }

    public List<AccountStatement> getByAccountId(Long accountId) {
        return accountStatementRepository.findByAccountId(accountId);
    }

}
