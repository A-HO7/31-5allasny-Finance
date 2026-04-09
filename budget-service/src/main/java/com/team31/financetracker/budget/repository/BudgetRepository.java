package com.team31.financetracker.budget.repository;

import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.BudgetStatus;
import com.team31.financetracker.budget.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE id = :userId", nativeQuery = true)
    boolean existsUserById(@Param("userId") Long userId);

    Optional<Budget> findFirstByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(
            Long userId,
            Category category,
            BudgetStatus status
    );

    @Modifying
    @Query(value = "DELETE FROM budgets WHERE status IN ('COMPLETED', 'EXCEEDED') AND created_at < :cutoffDate", nativeQuery = true)
    int purgeOldBudgets(@Param("cutoffDate") LocalDateTime cutoffDate);
}