package com.team31.financetracker.reporting.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cross-service native SQL repository for S5-F10 Financial Health Score.
 *
 * Queries the shared PostgreSQL database directly — accounts, transactions,
 * budgets, financial_goals, and users tables — without JPA entity mappings.
 * This follows the M1 cross-service native SQL pattern (no HTTP calls).
 */
@Repository
public class FinancialHealthRepository {

    @PersistenceContext
    private EntityManager em;

    // ── User check ───────────────────────────────────────────────────────────

    /**
     * Returns true if a user with the given ID exists in the shared users table.
     */
    public boolean existsUserById(Long userId) {
        Number count = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM users WHERE id = :userId")
                .setParameter("userId", userId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    // ── savingsRate data ─────────────────────────────────────────────────────

    /**
     * Returns [totalIncome, totalExpenses] for COMPLETED transactions
     * owned by the user's accounts within the given timestamp range,
     * filtered on transaction_date.
     *
     * Row: [0]=totalIncome (Double), [1]=totalExpenses (Double)
     */
    public Object[] querySavingsRateData(Long userId, LocalDateTime start, LocalDateTime end) {
        return (Object[]) em.createNativeQuery(
                "SELECT " +
                "  COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0) AS totalIncome, " +
                "  COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) AS totalExpenses " +
                "FROM transactions t " +
                "JOIN accounts a ON t.account_id = a.id " +
                "WHERE a.user_id = :userId " +
                "  AND t.status = 'COMPLETED' " +
                "  AND t.transaction_date >= :start " +
                "  AND t.transaction_date <= :end")
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
    }

    // ── budgetAdherenceRate data ──────────────────────────────────────────────

    /**
     * Returns rows for the user's budgets whose createdAt falls within range.
     * Each row: [0]=budget_amount, [1]=spent_amount, [2]=healthWeight (String from JSONB, may be null)
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> queryBudgetData(Long userId, LocalDateTime start, LocalDateTime end) {
        return em.createNativeQuery(
                "SELECT b.amount, b.spent_amount, " +
                "  (b.metadata->>'healthWeight') AS healthWeight " +
                "FROM budgets b " +
                "WHERE b.user_id = :userId " +
                "  AND b.created_at >= :start " +
                "  AND b.created_at <= :end")
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    // ── goalProgressRate data ─────────────────────────────────────────────────

    /**
     * Returns rows for non-completed financial goals (deadline >= today AND currentAmount < targetAmount).
     * Each row: [0]=currentAmount, [1]=targetAmount
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> queryGoalData(Long userId) {
        return em.createNativeQuery(
                "SELECT fg.current_amount, fg.target_amount " +
                "FROM financial_goals fg " +
                "WHERE fg.user_id = :userId " +
                "  AND fg.deadline >= CURRENT_DATE " +
                "  AND fg.current_amount < fg.target_amount " +
                "  AND fg.target_amount > 0")
                .setParameter("userId", userId)
                .getResultList();
    }

    // ── accountLiquidityRate data ─────────────────────────────────────────────

    /**
     * Returns total balance across the user's ACTIVE accounts.
     */
    public double queryTotalActiveBalance(Long userId) {
        Number result = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(a.balance), 0) " +
                "FROM accounts a " +
                "WHERE a.user_id = :userId " +
                "  AND a.status = 'ACTIVE'")
                .setParameter("userId", userId)
                .getSingleResult();
        return result.doubleValue();
    }

    /**
     * Returns total COMPLETED EXPENSE amount for the user's accounts within range.
     */
    public double queryTotalExpenseInRange(Long userId, LocalDateTime start, LocalDateTime end) {
        Number result = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(t.amount), 0) " +
                "FROM transactions t " +
                "JOIN accounts a ON t.account_id = a.id " +
                "WHERE a.user_id = :userId " +
                "  AND t.type = 'EXPENSE' " +
                "  AND t.status = 'COMPLETED' " +
                "  AND t.transaction_date >= :start " +
                "  AND t.transaction_date <= :end")
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        return result.doubleValue();
    }
}
