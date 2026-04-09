package com.team31.financetracker.budget.repository;

import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.BudgetStatus;
import com.team31.financetracker.budget.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE id = :userId", nativeQuery = true)
    boolean existsUserById(@Param("userId") Long userId);

    Optional<Budget> findFirstByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(
            Long userId,
            Category category,
            BudgetStatus status
    );

        @Query(value = """
            SELECT *
            FROM budgets
            WHERE jsonb_extract_path_text(metadata, :key) = :value
            """, nativeQuery = true)
        List<Budget> findByMetadataKeyEquals(@Param("key") String key, @Param("value") String value);

        @Query(value = """
            SELECT *
            FROM budgets
            WHERE jsonb_extract_path_text(metadata, :key) IS NOT NULL
              AND jsonb_extract_path_text(metadata, :key) ~ '^-?\\d+(\\.\\d+)?$'
              AND CAST(jsonb_extract_path_text(metadata, :key) AS double precision) > CAST(:value AS double precision)
            """, nativeQuery = true)
        List<Budget> findByMetadataKeyGreaterThan(@Param("key") String key, @Param("value") String value);

        @Query(value = """
            SELECT *
            FROM budgets
            WHERE jsonb_extract_path_text(metadata, :key) IS NOT NULL
              AND jsonb_extract_path_text(metadata, :key) ~ '^-?\\d+(\\.\\d+)?$'
              AND CAST(jsonb_extract_path_text(metadata, :key) AS double precision) < CAST(:value AS double precision)
            """, nativeQuery = true)
        List<Budget> findByMetadataKeyLessThan(@Param("key") String key, @Param("value") String value);

            @Query("""
              SELECT b
              FROM Budget b
              WHERE b.createdAt >= :startDateTime
                AND b.createdAt < :endDateTimeExclusive
                AND (:category IS NULL OR b.category = :category)
              ORDER BY b.createdAt ASC
              """)
            List<Budget> findHistoryByCreatedAtRange(
              @Param("startDateTime") java.time.LocalDateTime startDateTime,
              @Param("endDateTimeExclusive") java.time.LocalDateTime endDateTimeExclusive,
              @Param("category") Category category
            );
}