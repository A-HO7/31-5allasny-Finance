package com.team31.financetracker.transaction.repository;

import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

       // ── F1: search by date range (no status filter) ───────────────────────────
       @Query("SELECT t FROM Transaction t " +
                     "WHERE t.transactionDate >= :start AND t.transactionDate < :endExclusive " +
                     "ORDER BY t.transactionDate DESC")
       List<Transaction> findByTransactionDateRange(
                     @Param("start") LocalDateTime start,
                     @Param("endExclusive") LocalDateTime endExclusive);

       // ── F1: search by date range + status ─────────────────────────────────────
       @Query("SELECT t FROM Transaction t " +
                     "WHERE t.status = :status " +
                     "AND t.transactionDate >= :start AND t.transactionDate < :endExclusive " +
                     "ORDER BY t.transactionDate DESC")
       List<Transaction> findByStatusAndTransactionDateRange(
                     @Param("status") TransactionStatus status,
                     @Param("start") LocalDateTime start,
                     @Param("endExclusive") LocalDateTime endExclusive);

       // ── F2: approve — look up approver role in users table (cross-service) ────
       @Query(value = "SELECT role FROM users WHERE id = :userId", nativeQuery = true)
       Optional<String> findUserRoleById(@Param("userId") Long userId);

       // ── F2/F7: update account balance (cross-service, accounts table) ─────────
       @Transactional
       @Modifying
       @Query(value = "UPDATE accounts SET balance = balance + :amount WHERE id = :accountId", nativeQuery = true)
       int addToAccountBalance(@Param("accountId") Long accountId,
                     @Param("amount") Double amount);

       @Transactional
       @Modifying
       @Query(value = "UPDATE accounts SET balance = balance - :amount WHERE id = :accountId", nativeQuery = true)
       int subtractFromAccountBalance(@Param("accountId") Long accountId,
                     @Param("amount") Double amount);

       // ── F3: count active transactions in amount range for fee tier ────────────
       @Query(value = "SELECT COUNT(*) FROM transactions " +
                     "WHERE status IN ('PENDING', 'APPROVED') " +
                     "AND amount >= :minAmount AND amount <= :maxAmount", nativeQuery = true)
       long countActiveSimilarAmountTransactions(@Param("minAmount") Double minAmount,
                     @Param("maxAmount") Double maxAmount);

       // ── F4: update budget spent amount when expense is completed ─────────────
       @Transactional
       @Modifying
       @Query(value = "UPDATE budgets SET spent_amount = spent_amount + :amount " +
                     "WHERE category = :category " +
                     "AND :transactionDate >= start_date AND :transactionDate <= end_date", nativeQuery = true)
       void updateBudgetSpentAmount(@Param("amount") Double amount,
                     @Param("category") String category,
                     @Param("transactionDate") LocalDate transactionDate);

       // ── F5: filter transactions by JSONB metadata key=value ──────────────────
       @Query(value = "SELECT * FROM transactions t WHERE t.metadata ->> :key = :value", nativeQuery = true)
       List<Transaction> findByMetadataKeyValue(@Param("key") String key,
                     @Param("value") String value);

       // ── F6: analytics — single native SQL with conditional aggregation ────────
       @Query(value = "SELECT " +
                     "  COUNT(*)                                                                           AS totalTransactions, "
                     +
                     "  COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END)                                  AS completedTransactions, "
                     +
                     "  COUNT(CASE WHEN status = 'VOIDED'    THEN 1 END)                                  AS voidedTransactions, "
                     +
                     "  COALESCE(SUM(CASE WHEN type = 'INCOME'  AND status = 'COMPLETED' THEN amount ELSE 0 END), 0) AS totalIncome, "
                     +
                     "  COALESCE(SUM(CASE WHEN type = 'EXPENSE' AND status = 'COMPLETED' THEN amount ELSE 0 END), 0) AS totalExpenses "
                     +
                     "FROM transactions " +
                     "WHERE transaction_date >= :start AND transaction_date < :endExclusive", nativeQuery = true)
       Map<String, Object> getTransactionAnalytics(@Param("start") LocalDateTime start,
                     @Param("endExclusive") LocalDateTime endExclusive);

       @Query("SELECT t.category, COUNT(t) FROM Transaction t " +
                     "WHERE t.transactionDate >= :start AND t.transactionDate < :endExclusive " +
                     "GROUP BY t.category")
       List<Object[]> countTransactionsByCategory(@Param("start") LocalDateTime start,
                     @Param("endExclusive") LocalDateTime endExclusive);

       @Query("SELECT t.status, COUNT(t) FROM Transaction t " +
                     "WHERE t.transactionDate >= :start AND t.transactionDate < :endExclusive " +
                     "GROUP BY t.status")
       List<Object[]> countTransactionsByStatus(@Param("start") LocalDateTime start,
                     @Param("endExclusive") LocalDateTime endExclusive);

       // ── S3-F11: get userId for transaction via accounts join ───────────────────

       @Query(value = "SELECT a.user_id FROM transactions t JOIN accounts a ON t.account_id = a.id WHERE t.id = :transactionId", nativeQuery = true)
       Optional<Long> findUserIdByTransactionId(@Param("transactionId") Long transactionId);

       // ── S3-F11/S3-F12: get user details for Neo4j node creation ────────────────

       @Query(value = "SELECT name, preferences FROM users WHERE id = :userId", nativeQuery = true)
       Optional<Map<String, Object>> findUserDetailsById(@Param("userId") Long userId);

       // ── S3-F12: check if user exists ────────────────────────────────────────────

       @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE id = :userId", nativeQuery = true)
       boolean existsUserById(@Param("userId") Long userId);
}