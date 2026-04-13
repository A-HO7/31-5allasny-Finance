package com.team31.financetracker.budget.repository;

import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query(value = "SELECT true", nativeQuery = true)
    boolean existsUserById(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM budgets WHERE status IN ('COMPLETED', 'EXCEEDED') AND created_at < :cutoffDate", nativeQuery = true)
    int purgeOldBudgets(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query(value = "SELECT " +
            "CAST(COUNT(*) AS integer) AS totalBudgets, " +
            "COALESCE(SUM(amount), 0.0) AS totalBudgeted, " +
            "COALESCE(SUM(spent_amount), 0.0) AS totalSpent, " +
            "COALESCE(AVG(spent_amount / NULLIF(amount, 0)), 0.0) AS averageUtilization, " +
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
            "CAST(b.user_id AS varchar) AS userName, " +
            "b.category AS category, " +
            "b.amount AS budgetAmount, " +
            "b.spent_amount AS spentAmount, " +
            "(((b.spent_amount - b.amount) / NULLIF(b.amount, 0)) * 100) AS overspendPercentage, " +
            "COALESCE(CAST(b.metadata->>'warningSent' AS boolean), false) AS warningSent " +
            "FROM budgets b " +
            "WHERE b.spent_amount > b.amount " +
            "AND (((b.spent_amount - b.amount) / NULLIF(b.amount, 0)) * 100) >= :minOverspend " +
            "AND (:warningNotSent = false OR b.metadata->>'warningSent' IS NULL OR b.metadata->>'warningSent' = 'false')", 
            nativeQuery = true)
    java.util.List<com.team31.financetracker.budget.dto.OverspentBudgetProjection> findOverspentBudgets(
            @Param("minOverspend") Double minOverspend,
            @Param("warningNotSent") Boolean warningNotSent
    );

    @Query(value = "SELECT * FROM budgets WHERE user_id = :userId AND category = :#{#category.name()} AND status = 'ACTIVE' ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<Budget> findActiveBudgetForUserNative(
            @Param("userId") Long userId,
            @Param("category") Category category
    );

    @Query(value = "SELECT * FROM budgets WHERE " +
           "CASE " +
           "  WHEN :operator = 'eq' THEN metadata->>:key = :val " +
           "  WHEN :operator = 'gt' THEN CAST(metadata->>:key AS numeric) > CAST(:val AS numeric) " +
           "  WHEN :operator = 'lt' THEN CAST(metadata->>:key AS numeric) < CAST(:val AS numeric) " +
           "END", nativeQuery = true)
    List<Budget> searchBudgetsByMetadata(@Param("key") String key, @Param("operator") String operator, @Param("val") String val);

    @Query(value = "SELECT * FROM budgets WHERE created_at >= :startDate AND created_at <= :endDate " +
           "AND (:category IS NULL OR category = :category) ORDER BY created_at ASC", nativeQuery = true)
    List<Budget> findBudgetsInDateRange(@Param("startDate") java.time.LocalDateTime startDate, 
                                        @Param("endDate") java.time.LocalDateTime endDate, 
                                        @Param("category") String category);
    @Query(value = """
    SELECT
        b.id AS budget_id,
        CAST(b.user_id AS varchar) AS user_name,
        b.category AS category,
        b.amount AS budget_amount,
        b.spent_amount AS spent_amount,
        (b.spent_amount / NULLIF(b.amount, 0)) * 100 AS percent_used,
        (b.amount - b.spent_amount) AS remaining_amount
    FROM budgets b
    WHERE (b.spent_amount / NULLIF(b.amount, 0)) >= :threshold
      AND (:status IS NULL OR b.status = CAST(:status AS varchar))
    ORDER BY percent_used DESC
    """, nativeQuery = true)
    List<Object[]> findBudgetsNearLimit(@Param("threshold") Double threshold,
                                        @Param("status") String status);
}