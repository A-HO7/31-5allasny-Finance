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
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    List<Account> findByType(AccountType type);
    List<Account> findByStatus(AccountStatus status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE accounts SET balance = balance + :amount WHERE id = :id", nativeQuery = true)
    int updateBalance(@Param("id") Long id, @Param("amount") Double amount);

    @Modifying
    @Transactional
    @Query(value = "UPDATE accounts SET total_transactions = GREATEST(total_transactions - 1, 0) WHERE id = :id", nativeQuery = true)
    int decrementTotalTransactions(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE accounts SET total_transactions = total_transactions + 1, last_transaction_date = :timestamp WHERE id = :id", nativeQuery = true)
    int incrementTransactionStats(@Param("id") Long id, @Param("timestamp") LocalDateTime timestamp);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE accounts
    SET status = :status::text
    WHERE id = :id
    """, nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

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

    @Query(value = "SELECT a.id, a.name, a.balance, a.total_transactions as totalTransactions " +
            "FROM accounts a " +
            "ORDER BY a.balance DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopBalanceAccountsNative(@Param("limit") int limit);

    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.accountStatements WHERE a.id = :id")
    Optional<Account> findByIdWithStatements(@Param("id") Long id);

    @Query(value = """
    SELECT COUNT(s.id)
    FROM account_statements s
    WHERE s.account_id = :accountId
    AND s.expiry_date > CURRENT_DATE
    """, nativeQuery = true)
    Long getActiveStatementsCount(@Param("accountId") Long accountId);

    long countByIdIn(List<Long> ids);

    List<Account> findByUserIdAndStatus(Long userId, AccountStatus status);


}
