package com.team31.financetracker.account.service;

import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatement;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import com.team31.financetracker.account.repository.AccountRepository;
import com.team31.financetracker.account.repository.AccountStatementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountStatementRepository accountStatementRepository;

    public AccountService(AccountRepository accountRepository,
                          AccountStatementRepository accountStatementRepository) {
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

    public List<Account> searchByStatusAndBalanceRange(AccountStatus status, Double minBalance, Double maxBalance) {
        if (minBalance != null && maxBalance != null && minBalance > maxBalance) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid balance range");
        }
        String statusParam = status != null ? status.name() : null;
        return accountRepository.searchByStatusAndBalanceRange(statusParam, minBalance, maxBalance);
    }

    @Transactional
    public void freezeAccount(Long id, AccountStatus newStatus) {
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        Account account = getById(id);
        if (newStatus == AccountStatus.FROZEN) {
            long pending = accountRepository.countPendingTransactionsForAccount(id);
            if (pending > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot freeze account with pending transactions");
            }
        }
        account.setStatus(newStatus);
        accountRepository.save(account);
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
}
