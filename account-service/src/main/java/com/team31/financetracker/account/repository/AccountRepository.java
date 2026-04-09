package com.team31.financetracker.account.repository;

import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByUserId(Long userId);
    List<Account> findByType(AccountType type);
    List<Account> findByStatus(AccountStatus status);
}
