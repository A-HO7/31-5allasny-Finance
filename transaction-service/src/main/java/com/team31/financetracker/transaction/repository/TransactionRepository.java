package com.team31.financetracker.transaction.repository;

import com.team31.financetracker.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Modifying
    @Query(value = "UPDATE budgets SET spent_amount = spent_amount + :amount " +
            "WHERE category = :category AND :transactionDate >= start_date AND :transactionDate <= end_date", nativeQuery = true)
    void updateBudgetSpentAmount(@Param("amount") Double amount, @Param("category") String category,
            @Param("transactionDate") LocalDate transactionDate);
}
