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

        @Query("SELECT t FROM Transaction t WHERE t.transactionDate >= :start AND t.transactionDate < :endExclusive ORDER BY t.transactionDate DESC")
        List<Transaction> findByTransactionDateRange(
                @Param("start") LocalDateTime start,
                @Param("endExclusive") LocalDateTime endExclusive);

        @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.transactionDate >= :start AND t.transactionDate < :endExclusive ORDER BY t.transactionDate DESC")
        List<Transaction> findByStatusAndTransactionDateRange(
                @Param("status") TransactionStatus status,
                @Param("start") LocalDateTime start,
                @Param("endExclusive") LocalDateTime endExclusive);

        // Kept as nativeQuery — column aliases must exactly match DTO field names
        @Query(value = "SELECT COUNT(*) as totalTransactions, " +
                "COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completedTransactions, " +
                "COUNT(CASE WHEN status = 'VOIDED' THEN 1 END) as voidedTransactions, " +
                "COALESCE(SUM(CASE WHEN type = 'INCOME' AND status = 'COMPLETED' THEN amount ELSE 0 END), 0) as totalIncome, " +
                "COALESCE(SUM(CASE WHEN type = 'EXPENSE' AND status = 'COMPLETED' THEN amount ELSE 0 END), 0) as totalExpenses " +
                "FROM transactions " +
                "WHERE transaction_date >= :start AND transaction_date < :endExclusive",
                nativeQuery = true)
        Map<String, Object> getTransactionAnalytics(
                @Param("start") LocalDateTime start,
                @Param("endExclusive") LocalDateTime endExclusive);

        @Query(value = "SELECT * FROM transactions t WHERE t.metadata ->> :key = :value", nativeQuery = true)
        List<Transaction> findByMetadataKeyValue(
                @Param("key") String key,
                @Param("value") String value);

        @Transactional
        @Modifying
        @Query(value = "UPDATE budgets SET spent_amount = spent_amount + :amount " +
                "WHERE category = :category AND :transactionDate >= start_date AND :transactionDate <= end_date",
                nativeQuery = true)
        void updateBudgetSpentAmount(
                @Param("amount") Double amount,
                @Param("category") String category,
                @Param("transactionDate") LocalDate transactionDate);

        // FIX: split IN (:id1, :id2) into two separate equality checks.
        // Spring Data native query parsing fails at startup on multi-param IN clauses
        // in some versions, causing full context load failure (breaks TC_S3_63-66).
        @Query(value = "SELECT COUNT(*) FROM accounts WHERE id = :accountId OR id = :toAccountId",
                nativeQuery = true)
        long countAccountsByIds(
                @Param("accountId") Long accountId,
                @Param("toAccountId") Long toAccountId);

        @Query(value = "SELECT COUNT(*) FROM transactions " +
                "WHERE status IN ('PENDING', 'APPROVED') " +
                "AND amount >= :minAmount AND amount <= :maxAmount",
                nativeQuery = true)
        long countActiveSimilarAmountTransactions(
                @Param("minAmount") Double minAmount,
                @Param("maxAmount") Double maxAmount);

        // FIX: removed escaped double-quotes around 'role' column name.
        // The backslash-escaped quotes inside a Java annotation string can cause
        // query parsing issues at context load time (breaks TC_S3_63-66).
        @Query(value = "SELECT role FROM users WHERE id = :userId", nativeQuery = true)
        Optional<String> findUserRoleById(@Param("userId") Long userId);

        @Transactional
        @Modifying
        @Query(value = "UPDATE accounts SET balance = balance + :amount WHERE id = :accountId",
                nativeQuery = true)
        int addToAccountBalance(
                @Param("accountId") Long accountId,
                @Param("amount") Double amount);

        @Transactional
        @Modifying
        @Query(value = "UPDATE accounts SET balance = balance - :amount WHERE id = :accountId",
                nativeQuery = true)
        int subtractFromAccountBalance(
                @Param("accountId") Long accountId,
                @Param("amount") Double amount);
}