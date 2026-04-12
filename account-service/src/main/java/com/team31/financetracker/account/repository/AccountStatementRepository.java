package com.team31.financetracker.account.repository;

import com.team31.financetracker.account.model.AccountStatement;
import com.team31.financetracker.account.model.StatementType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountStatementRepository extends JpaRepository<AccountStatement, Long> {
    List<AccountStatement> findByType(StatementType type);

    List<AccountStatement> findByExpiryDateBefore(java.time.LocalDate date);
    List<AccountStatement> findByExpiryDateAfter(java.time.LocalDate date);

    @Query(value = """
            SELECT s.*
            FROM account_statements s
            JOIN accounts a ON a.id = s.account_id
            WHERE a.user_id = :userId
            """, nativeQuery = true)
    List<AccountStatement> findByUserId(@Param("userId") Long userId);
    List<AccountStatement> findByAccountId(Long accountId);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM account_statements
            WHERE expiry_date < :cutoffDate
            """, nativeQuery = true)
    int deleteExpiredBefore(@Param("cutoffDate") java.time.LocalDate cutoffDate);



}