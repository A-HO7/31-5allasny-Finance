package com.team31.financetracker.transaction.repository;

import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Query(value = """
            SELECT
                COUNT(*) as totalTransactions,
                COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completedTransactions,
                COUNT(CASE WHEN status = 'VOIDED' THEN 1 END) as voidedTransactions,
                COALESCE(SUM(CASE WHEN type = 'INCOME' AND status = 'COMPLETED' THEN amount ELSE 0 END), 0) as totalIncome,
                COALESCE(SUM(CASE WHEN type = 'EXPENSE' AND status = 'COMPLETED' THEN amount ELSE 0 END), 0) as totalExpenses
            FROM transactions
            WHERE transaction_date >= :start AND transaction_date < :endExclusive
            """, nativeQuery = true)
    Map<String, Object> getTransactionAnalytics(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive);

    @Query(value = "SELECT * FROM transactions t WHERE t.metadata @> jsonb_build_object(:key, :value::jsonb)", nativeQuery = true)
    List<Transaction> findByMetadataKeyValue(
            @Param("key") String key,
            @Param("value") String value);
    @Modifying
    @Query(value = "UPDATE budgets SET spent_amount = spent_amount + :amount " +
            "WHERE category = :category AND :transactionDate >= start_date AND :transactionDate <= end_date", nativeQuery = true)
    void updateBudgetSpentAmount(@Param("amount") Double amount, @Param("category") String category,
            @Param("transactionDate") LocalDate transactionDate);
}
