package com.team31.financetracker.account.service;

import com.team31.financetracker.account.dto.AccountSummaryDTO;
import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import com.team31.financetracker.account.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account create(Account account) {
        return accountRepository.save(account);
    }

    public Account getById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    public Account getByUserId(Long userId) {
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
    public AccountSummaryDTO getSummary(Long id, LocalDateTime start, LocalDateTime end) {
        if (!accountRepository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Object[] result = (Object[]) accountRepository.getAccountSummaryNative(id, start, end);
        return new AccountSummaryDTO((Long)result[0], (String)result[1], (Double)result[2],
                (Double)result[3], (Double)result[2] - (Double)result[3], (Long)result[4]);
    }
}
