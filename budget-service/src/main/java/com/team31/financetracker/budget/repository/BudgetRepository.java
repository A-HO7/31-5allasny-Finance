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

    @org.springframework.data.jpa.repository.Modifying
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

    @Query(value = "SELECT " +
            "b.id AS budgetId, " +
            "u.name AS userName, " +
            "b.category AS category, " +
            "b.budget_amount AS budgetAmount, " +
            "b.spent_amount AS spentAmount, " +
            "(((b.spent_amount - NULLIF(b.budget_amount, 0)) / NULLIF(b.budget_amount, 0)) * 100) AS overspendPercentage, " +
            "COALESCE(CAST(b.metadata->>'warningSent' AS boolean), false) AS warningSent " +
            "FROM budgets b " +
            "JOIN users u ON b.user_id = u.id " +
            "WHERE b.spent_amount > b.budget_amount " +
            "AND (((b.spent_amount - NULLIF(b.budget_amount, 0)) / NULLIF(b.budget_amount, 0)) * 100) > :minOverspend " +
            "AND (:warningNotSent = false OR b.metadata->>'warningSent' IS NULL OR b.metadata->>'warningSent' = 'false')", 
            nativeQuery = true)
    java.util.List<com.team31.financetracker.budget.dto.OverspentBudgetProjection> findOverspentBudgets(
            @Param("minOverspend") Double minOverspend,
            @Param("warningNotSent") Boolean warningNotSent
    );

    Optional<Budget> findFirstByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(
            Long userId,
            Category category,
            BudgetStatus status
    );

    @Modifying
    @Query(value = "DELETE FROM budgets WHERE status IN ('COMPLETED', 'EXCEEDED') AND created_at < :cutoffDate", nativeQuery = true)
    int purgeOldBudgets(@Param("cutoffDate") LocalDateTime cutoffDate);
}