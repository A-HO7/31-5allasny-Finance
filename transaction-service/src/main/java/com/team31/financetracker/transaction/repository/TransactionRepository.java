package com.team31.financetracker.transaction.repository;

import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
}
