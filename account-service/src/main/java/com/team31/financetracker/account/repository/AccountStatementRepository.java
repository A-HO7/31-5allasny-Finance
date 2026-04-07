package com.team31.financetracker.account.repository;

import com.team31.financetracker.account.model.AccountStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountStatementRepository extends JpaRepository<AccountStatement, Long> {
}