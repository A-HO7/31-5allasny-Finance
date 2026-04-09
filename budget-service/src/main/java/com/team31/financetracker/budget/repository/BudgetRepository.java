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

    @Modifying
    @Query(value = "DELETE FROM budgets WHERE status IN ('COMPLETED', 'EXCEEDED') AND created_at < :cutoffDate", nativeQuery = true)
    int purgeOldBudgets(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query(value = "SELECT " +
            "CAST(COUNT(*) AS integer) AS totalBudgets, " +
            "COALESCE(SUM(budget_amount), 0.0) AS totalBudgeted, " +
            "COALESCE(SUM(spent_amount), 0.0) AS totalSpent, " +
            "COALESCE(AVG(spent_amount / NULLIF(budget_amount, 0)), 0.0) AS averageUtilization, " +
            "CAST(SUM(CASE WHEN status = 'EXCEEDED' THEN 1 ELSE 0 END) AS integer) AS exceededCount " +
            "FROM budgets " +
            "WHERE user_id = :userId AND created_at >= :startDate AND created_at <= :endDate", nativeQuery = true)
    com.team31.financetracker.budget.dto.PerformanceProjection getBudgetPerformanceAggregates(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    Optional<Budget> findFirstByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(
            Long userId,
            Category category,
            BudgetStatus status
    );
}