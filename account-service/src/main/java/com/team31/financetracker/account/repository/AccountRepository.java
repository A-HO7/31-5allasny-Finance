package com.team31.financetracker.account.repository;

import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    List<Account> findByType(AccountType type);
    List<Account> findByStatus(AccountStatus status);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE accounts
    SET status = :status
    WHERE id = :id
    """, nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    @Query(value = """
        SELECT a.*
        FROM accounts a
        JOIN users u ON u.id = a.user_id
        WHERE u.email = :email
    """, nativeQuery = true)
    List<Account> findByUserEmail(@Param("email") String email);

    @Query(value = """
            SELECT COUNT(*) FROM transactions t
            WHERE t.status::text = 'PENDING'
            AND (t.account_id = :accountId OR t.to_account_id = :accountId)
            """, nativeQuery = true)
    long countPendingTransactionsForAccount(@Param("accountId") Long accountId);

    @Query(value = """
            SELECT * FROM accounts
            WHERE (:status IS NULL OR status = :status)
            AND (:minBalance IS NULL OR balance >= :minBalance)
            AND (:maxBalance IS NULL OR balance <= :maxBalance)
            ORDER BY balance DESC
            """, nativeQuery = true)
    List<Account> searchByStatusAndBalanceRange(
            @Param("status") String status,
            @Param("minBalance") Double minBalance,
            @Param("maxBalance") Double maxBalance);

    @Query(value = "SELECT DISTINCT a.* FROM accounts a " +
            "JOIN account_statements s ON a.id = s.account_id " +
            "WHERE s.expiry_date < CURRENT_TIMESTAMP", nativeQuery = true)
    List<Account> findAccountsWithExpiredStatementsNative();

    @Query(value = """
            SELECT a.*
            FROM accounts a
            WHERE a.account_details ->> :key = :value
              AND (:status IS NULL OR a.status::text = :status)
            """, nativeQuery = true)
    List<Account> findByDetailKeyValueAndOptionalStatus(
            @Param("key") String key,
            @Param("value") String value,
            @Param("status") String status
    );

    @Query(value = "SELECT a.id, a.name, a.balance, COUNT(t.id) as totalTransactions " +
            "FROM accounts a " +
            "LEFT JOIN transactions t ON a.id = t.account_id " +
            "GROUP BY a.id, a.name, a.balance " +
            "ORDER BY a.balance DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopBalanceAccountsNative(@Param("limit") int limit);

    @Query(value = """
        SELECT COUNT(*) > 0
        FROM users u
        WHERE u.id = :userId
          AND u.role = 'ADMIN'
        """, nativeQuery = true)
    boolean isAdminUser(@Param("userId") Long userId);

    @Query(value = """
    SELECT a.id, a.name,
           COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0) as totalDeposits,
           COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) as totalWithdrawals,
           COUNT(t.id) as transactionCount
    FROM accounts a
    LEFT JOIN transactions t ON a.id = t.account_id
    WHERE a.id = :accountId AND t.transaction_date BETWEEN :start AND :end
    GROUP BY a.id, a.name
    """, nativeQuery = true)
    Object getAccountSummaryNative(@Param("accountId") Long accountId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
