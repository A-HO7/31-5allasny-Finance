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
import com.team31.financetracker.budget.dto.BudgetAnalyticsProjection;
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM users WHERE id = :userId", nativeQuery = true)
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

    @Query(value = "SELECT " +
            "b.id AS budgetId, " +
            "b.user_id AS userId, " +
            "CAST(b.user_id AS varchar) AS userName, " +
            "b.category AS category, " +
            "b.budget_amount AS budgetAmount, " +
            "b.spent_amount AS spentAmount, " +
            "(((b.spent_amount - b.budget_amount) / NULLIF(b.budget_amount, 0)) * 100) AS overspendPercentage, " +
            "COALESCE(CAST(b.metadata->>'warningSent' AS boolean), false) AS warningSent " +
            "FROM budgets b " +
            "WHERE b.spent_amount > b.budget_amount " +
            "AND (((b.spent_amount - b.budget_amount) / NULLIF(b.budget_amount, 0)) * 100) >= :minOverspend " +
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
        b.budget_amount AS budget_amount,
        b.spent_amount AS spent_amount,
        (b.spent_amount / NULLIF(b.budget_amount, 0)) * 100 AS percent_used,
        (b.budget_amount - b.spent_amount) AS remaining_amount
    FROM budgets b
    WHERE (b.spent_amount / NULLIF(b.budget_amount, 0)) >= :threshold
      AND (:status IS NULL OR b.status = CAST(:status AS varchar))
    ORDER BY percent_used DESC
    """, nativeQuery = true)
    List<Object[]> findBudgetsNearLimit(@Param("threshold") Double threshold,
                                        @Param("status") String status);
    @Query(value = """
    SELECT
        CAST(COUNT(*) AS bigint) AS totalBudgets,
        CAST(COALESCE(SUM(budget_amount), 0.0) AS double precision) AS totalBudgetAmount,
        CAST(COALESCE(SUM(spent_amount), 0.0) AS double precision) AS totalSpentAmount,
        CAST(COALESCE(AVG(
            CASE
                WHEN budget_amount = 0 THEN 0
                ELSE (spent_amount / budget_amount) * 100
            END
        ), 0.0) AS double precision) AS averageUtilization,
        CAST(SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS bigint) AS activeBudgets,
        CAST(SUM(CASE WHEN status = 'EXCEEDED' THEN 1 ELSE 0 END) AS bigint) AS exceededBudgets,
        CAST(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS bigint) AS completedBudgets
    FROM budgets
    WHERE user_id = :userId
    """, nativeQuery = true)
    BudgetAnalyticsProjection getBudgetAnalytics(@Param("userId") Long userId);

    @Query(value = """
    SELECT
        CAST(COUNT(*) AS bigint) AS totalBudgets,
        CAST(COALESCE(SUM(budget_amount), 0.0) AS double precision) AS totalBudgetAmount,
        CAST(COALESCE(SUM(spent_amount), 0.0) AS double precision) AS totalSpentAmount,
        CAST(COALESCE(AVG(
            CASE
                WHEN budget_amount = 0 THEN 0
                ELSE (spent_amount / budget_amount) * 100
            END
        ), 0.0) AS double precision) AS averageUtilization,
        CAST(SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS bigint) AS activeBudgets,
        CAST(SUM(CASE WHEN status = 'EXCEEDED' THEN 1 ELSE 0 END) AS bigint) AS exceededBudgets,
        CAST(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS bigint) AS completedBudgets
    FROM budgets
    WHERE user_id = :userId
      AND start_date >= :startDate
      AND end_date <= :endDate
    """, nativeQuery = true)
    BudgetAnalyticsProjection getBudgetAnalyticsByDateRange(
            @Param("userId") Long userId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    @Query(value = "SELECT status, COUNT(*) FROM budgets WHERE user_id = :userId AND start_date >= :startDate AND end_date <= :endDate GROUP BY status", nativeQuery = true)
    List<Object[]> countBudgetsByStatusByDateRange(@Param("userId") Long userId, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    @Query(value = "SELECT period, COUNT(*) FROM budgets WHERE user_id = :userId AND start_date >= :startDate AND end_date <= :endDate GROUP BY period", nativeQuery = true)
    List<Object[]> countBudgetsByPeriodByDateRange(@Param("userId") Long userId, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    @Query(value = "SELECT status, COUNT(*) FROM budgets WHERE user_id = :userId GROUP BY status", nativeQuery = true)
    List<Object[]> countBudgetsByStatus(@Param("userId") Long userId);

    @Query(value = "SELECT period, COUNT(*) FROM budgets WHERE user_id = :userId GROUP BY period", nativeQuery = true)
    List<Object[]> countBudgetsByPeriod(@Param("userId") Long userId);

    @Query(value = "SELECT CAST(COUNT(*) AS integer) FROM budgets WHERE user_id = :userId AND status = 'ACTIVE'", nativeQuery = true)
    int countActiveBudgetsByUserId(@Param("userId") Long userId);

    @Query(value = """
    SELECT
        CAST(COALESCE(SUM(budget_amount), 0.0) AS double precision) AS totalBudgeted,
        CAST(COALESCE(SUM(spent_amount), 0.0) AS double precision) AS totalSpent,
        CASE
            WHEN SUM(COALESCE(CAST(metadata->>'healthWeight' AS double precision), 1.0)) = 0 THEN 0.0
            ELSE SUM(
                CASE
                    WHEN spent_amount IS NULL OR spent_amount <= 0 THEN 1.0
                    WHEN spent_amount <= budget_amount THEN 1.0
                    ELSE CAST(budget_amount AS double precision) / spent_amount
                END
                * COALESCE(CAST(metadata->>'healthWeight' AS double precision), 1.0)
            ) / SUM(COALESCE(CAST(metadata->>'healthWeight' AS double precision), 1.0))
        END AS weightedAdherenceRate
    FROM budgets
    WHERE user_id = :userId
      AND (:startDate IS NULL OR start_date >= :startDate)
      AND (:endDate IS NULL OR end_date <= :endDate)
    """, nativeQuery = true)
    com.team31.financetracker.budget.dto.BudgetSummaryProjection getBudgetSummary(
            @Param("userId") Long userId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );
    @Modifying
    @Query(value = "UPDATE budgets SET spent_amount = spent_amount + :amount, status = (CASE WHEN spent_amount + :amount > budget_amount THEN 'EXCEEDED' ELSE status END) WHERE user_id = :userId AND category = :category AND status = 'ACTIVE'", nativeQuery = true)
    int applyTransactionCompleted(@Param("userId") Long userId, @Param("category") String category, @Param("amount") Double amount);

    @Modifying
    @Query(value = "UPDATE budgets SET spent_amount = GREATEST(spent_amount - :amount, 0), status = (CASE WHEN spent_amount - :amount <= budget_amount THEN 'ACTIVE' ELSE status END) WHERE user_id = :userId AND category = :category", nativeQuery = true)
    int applyTransactionVoided(@Param("userId") Long userId, @Param("category") String category, @Param("amount") Double amount);

    @Query(value = "SELECT * FROM budgets WHERE user_id = :userId AND category = :category ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<Budget> findLatestBudgetForUserNative(@Param("userId") Long userId, @Param("category") String category);
}


