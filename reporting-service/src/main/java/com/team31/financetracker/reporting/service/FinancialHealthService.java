package com.team31.financetracker.reporting.service;

import com.team31.financetracker.reporting.dto.FinancialHealthScoreDTO;
import com.team31.financetracker.reporting.mongo.ReportAuditEventRepository;
import com.team31.financetracker.reporting.observer.EntityObserver;
import com.team31.financetracker.reporting.observer.MongoEventLogger;
import com.team31.financetracker.reporting.repository.FinancialHealthRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for S5-F10: Get Financial Health Score.
 *
 * Design: Two public methods are intentionally separated:
 *   1. computeHealthScore() — @Cacheable, does the expensive cross-service SQL computation.
 *   2. logHealthScoreViewed() — NOT cached, always fires MongoDB ANALYTICS_VIEWED audit log.
 *
 * The controller calls BOTH on every request, ensuring the audit log fires
 * on cache hits as well as misses (per spec requirement h).
 */
@Service
public class FinancialHealthService {

    private static final Logger log = LoggerFactory.getLogger(FinancialHealthService.class);

    private final FinancialHealthRepository healthRepository;
    private final MongoEventLogger mongoEventLogger;

    private final List<EntityObserver> observers = new CopyOnWriteArrayList<>();

    public FinancialHealthService(FinancialHealthRepository healthRepository,
                                  MongoEventLogger mongoEventLogger) {
        this.healthRepository = healthRepository;
        this.mongoEventLogger = mongoEventLogger;
    }

    @PostConstruct
    public void init() {
        if (mongoEventLogger != null && !observers.contains(mongoEventLogger)) {
            observers.add(mongoEventLogger);
        }
    }

    // ── Cached computation ───────────────────────────────────────────────────

    /**
     * Computes the financial health score for the given user and date range.
     * Result is cached for 10 minutes in Redis under key: userId:startDate:endDate.
     *
     * Steps c, d, e, f, g from the spec.
     * Step c: user existence check.
     * Step d: date range expansion to full-day timestamps.
     * Step e: compute four component rates via cross-service native SQL.
     * Step f: compositeScore weighted sum, rounded to 1 decimal.
     * Step g: recommendations list.
     */
    @Cacheable(value = "reporting-service::S5-F10",
               key = "#userId + ':' + #startDate + ':' + #endDate")
    public FinancialHealthScoreDTO computeHealthScore(Long userId,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        // Step c — user existence
        if (!healthRepository.existsUserById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        // Step d — expand to closed timestamp window
        LocalDateTime windowStart = startDate.atStartOfDay();
        LocalDateTime windowEnd   = endDate.atTime(23, 59, 59, 999_000_000);

        // Step e — compute component rates
        double savingsRate          = computeSavingsRate(userId, windowStart, windowEnd);
        double budgetAdherenceRate  = computeBudgetAdherenceRate(userId, windowStart, windowEnd);
        double goalProgressRate     = computeGoalProgressRate(userId);
        double accountLiquidityRate = computeAccountLiquidityRate(userId, startDate, endDate, windowStart, windowEnd);

        // Step f — composite score
        double raw = 0.35 * savingsRate
                   + 0.30 * budgetAdherenceRate
                   + 0.20 * goalProgressRate
                   + 0.15 * accountLiquidityRate;
        double compositeScore = Math.round(raw * 10.0) / 10.0;

        // Step g — recommendations
        List<String> recommendations = buildRecommendations(
                savingsRate, budgetAdherenceRate, goalProgressRate, accountLiquidityRate);

        return FinancialHealthScoreDTO.builder()
                .userId(userId)
                .compositeScore(compositeScore)
                .savingsRate(savingsRate)
                .budgetAdherenceRate(budgetAdherenceRate)
                .goalProgressRate(goalProgressRate)
                .accountLiquidityRate(accountLiquidityRate)
                .recommendations(recommendations)
                .build();
    }

    // ── Audit log (NOT cached — always fires) ────────────────────────────────

    /**
     * Logs an ANALYTICS_VIEWED event to MongoDB.
     * Called by the controller on EVERY request, including cache hits.
     * Soft dependency: never throws — failures are logged at WARN level.
     */
    public void logHealthScoreViewed(Long userId) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("reportId", -1L);          // sentinel: no specific report
            payload.put("reportType", null);
            payload.put("pagesGenerated", null);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("feature", "S5-F10");
            details.put("userId", userId);
            payload.put("details", details);

            for (EntityObserver observer : observers) {
                observer.onEvent("ANALYTICS_VIEWED", payload);
            }
        } catch (Exception e) {
            log.warn("FinancialHealthService: failed to log ANALYTICS_VIEWED for userId {}: {}",
                    userId, e.getMessage());
        }
    }

    // ── Private computation helpers ──────────────────────────────────────────

    /**
     * savingsRate = 100 * (totalIncome - totalExpenses) / totalIncome
     * Returns 0 when totalIncome = 0. Clamped to [0, 100].
     */
    private double computeSavingsRate(Long userId, LocalDateTime start, LocalDateTime end) {
        try {
            Object[] row = healthRepository.querySavingsRateData(userId, start, end);
            double totalIncome   = row[0] != null ? ((Number) row[0]).doubleValue() : 0.0;
            double totalExpenses = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            if (totalIncome == 0.0) return 0.0;
            double rate = 100.0 * (totalIncome - totalExpenses) / totalIncome;
            return clamp(rate);
        } catch (Exception e) {
            log.warn("S5-F10 savingsRate query failed: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * budgetAdherenceRate = weighted average of 100 * max(0, 1 - spentAmount/budgetAmount)
     * Weight = metadata.healthWeight (default 1.0, clamped [0.0, 2.0]).
     * Budgets with budgetAmount=0 or healthWeight=0 are excluded.
     * Returns 0 when no qualifying budgets.
     */
    private double computeBudgetAdherenceRate(Long userId, LocalDateTime start, LocalDateTime end) {
        try {
            List<Object[]> rows = healthRepository.queryBudgetData(userId, start, end);
            double weightedSum   = 0.0;
            double totalWeight   = 0.0;

            for (Object[] row : rows) {
                double budgetAmount = row[0] != null ? ((Number) row[0]).doubleValue() : 0.0;
                double spentAmount  = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                double healthWeight = parseHealthWeight(row[2]);

                if (budgetAmount == 0.0 || healthWeight == 0.0) continue;

                double adherence = 100.0 * Math.max(0.0, 1.0 - (spentAmount / budgetAmount));
                weightedSum += adherence * healthWeight;
                totalWeight += healthWeight;
            }

            if (totalWeight == 0.0) return 0.0;
            return clamp(weightedSum / totalWeight);
        } catch (Exception e) {
            log.warn("S5-F10 budgetAdherenceRate query failed: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * goalProgressRate = average of 100 * min(1, currentAmount/targetAmount)
     * across non-completed goals (deadline >= today, currentAmount < targetAmount).
     * Returns 0 when no qualifying goals.
     */
    private double computeGoalProgressRate(Long userId) {
        try {
            List<Object[]> rows = healthRepository.queryGoalData(userId);
            if (rows.isEmpty()) return 0.0;

            double sum = 0.0;
            for (Object[] row : rows) {
                double current = row[0] != null ? ((Number) row[0]).doubleValue() : 0.0;
                double target  = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                if (target <= 0.0) continue;
                sum += 100.0 * Math.min(1.0, current / target);
            }
            return clamp(sum / rows.size());
        } catch (Exception e) {
            log.warn("S5-F10 goalProgressRate query failed: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * accountLiquidityRate = 100 * min(1, totalActiveBalance / (3 * monthlyAverage))
     * monthlyAverage = totalExpenseInRange * 30.0 / daysInRange
     * daysInRange = ChronoUnit.DAYS.between(startDate, endDate) + 1
     * Returns 100 if no COMPLETED EXPENSE transactions in range (fully liquid by default).
     */
    private double computeAccountLiquidityRate(Long userId,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                LocalDateTime windowStart,
                                                LocalDateTime windowEnd) {
        try {
            double totalExpense = healthRepository.queryTotalExpenseInRange(userId, windowStart, windowEnd);
            if (totalExpense == 0.0) return 100.0;

            long daysInRange    = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            double monthlyAvg   = totalExpense * 30.0 / daysInRange;
            double threeMthAvg  = 3.0 * monthlyAvg;

            double totalBalance = healthRepository.queryTotalActiveBalance(userId);
            double rate = 100.0 * Math.min(1.0, totalBalance / threeMthAvg);
            return clamp(rate);
        } catch (Exception e) {
            log.warn("S5-F10 accountLiquidityRate query failed: {}", e.getMessage());
            return 100.0;
        }
    }

    /**
     * Parses the healthWeight value extracted from JSONB (may be String, Number, or null).
     * Defaults to 1.0 when missing or unparseable. Clamped to [0.0, 2.0].
     */
    private double parseHealthWeight(Object raw) {
        if (raw == null) return 1.0;
        double val;
        if (raw instanceof Number n) {
            val = n.doubleValue();
        } else {
            try {
                val = Double.parseDouble(raw.toString());
            } catch (Exception e) {
                return 1.0;
            }
        }
        return Math.min(2.0, Math.max(0.0, val));
    }

    /**
     * Clamps a rate to [0.0, 100.0].
     */
    private double clamp(double rate) {
        return Math.min(100.0, Math.max(0.0, rate));
    }

    /**
     * Builds the recommendations list.
     * One canned message per component below 50.
     * Returns a single positive string if all components are >= 50.
     */
    private List<String> buildRecommendations(double savingsRate,
                                              double budgetAdherenceRate,
                                              double goalProgressRate,
                                              double accountLiquidityRate) {
        List<String> recs = new ArrayList<>();
        if (savingsRate         < 50) recs.add("Increase savings rate: income:expense ratio is low.");
        if (budgetAdherenceRate < 50) recs.add("Revisit budgets: several categories are overspending.");
        if (goalProgressRate    < 50) recs.add("Set or update financial goals: progress is limited.");
        if (accountLiquidityRate< 50) recs.add("Build liquidity: accessible balance is below 3 months of expenses.");
        if (recs.isEmpty()) recs.add("Great financial health! All indicators are on track.");
        return recs;
    }
}
