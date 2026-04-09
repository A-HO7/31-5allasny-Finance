package com.team31.financetracker.account.repository;

import com.team31.financetracker.account.model.AccountStatement;
import com.team31.financetracker.account.model.StatementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountStatementRepository extends JpaRepository<AccountStatement, Long> {
    List<AccountStatement> findByType(StatementType type);

    List<AccountStatement> findByExpiryDateBefore(java.time.LocalDate date);
    List<AccountStatement> findByExpiryDateAfter(java.time.LocalDate date);

}