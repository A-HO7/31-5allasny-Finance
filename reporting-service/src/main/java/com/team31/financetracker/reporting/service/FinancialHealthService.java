package com.team31.financetracker.reporting.service;

import com.team31.financetracker.contracts.dto.AccountBalanceSummaryDTO;
import com.team31.financetracker.contracts.dto.BudgetSummaryDTO;
import com.team31.financetracker.contracts.dto.FinancialGoalDTO;
import com.team31.financetracker.contracts.dto.NetIncomeDTO;
import com.team31.financetracker.contracts.dto.UserProfileDTO;
import com.team31.financetracker.contracts.feign.AccountServiceClient;
import com.team31.financetracker.contracts.feign.BudgetServiceClient;
import com.team31.financetracker.contracts.feign.TransactionServiceClient;
import com.team31.financetracker.contracts.feign.UserServiceClient;
import com.team31.financetracker.reporting.dto.FinancialHealthScoreDTO;
import com.team31.financetracker.reporting.exception.ServiceUnavailableException;
import com.team31.financetracker.reporting.observer.EntityObserver;
import com.team31.financetracker.reporting.observer.MongoEventLogger;
import feign.FeignException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for S5-F10: Get Financial Health Score.
 */
@Service
public class FinancialHealthService {

    private static final Logger log = LoggerFactory.getLogger(FinancialHealthService.class);

    private final UserServiceClient userServiceClient;
    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final BudgetServiceClient budgetServiceClient;
    private final MongoEventLogger mongoEventLogger;

    private final List<EntityObserver> observers = new CopyOnWriteArrayList<>();

    public FinancialHealthService(UserServiceClient userServiceClient,
                                  AccountServiceClient accountServiceClient,
                                  TransactionServiceClient transactionServiceClient,
                                  BudgetServiceClient budgetServiceClient,
                                  MongoEventLogger mongoEventLogger) {
        this.userServiceClient = userServiceClient;
        this.accountServiceClient = accountServiceClient;
        this.transactionServiceClient = transactionServiceClient;
        this.budgetServiceClient = budgetServiceClient;
        this.mongoEventLogger = mongoEventLogger;
    }

    @PostConstruct
    public void init() {
        if (mongoEventLogger != null && !observers.contains(mongoEventLogger)) {
            observers.add(mongoEventLogger);
        }
    }

    // ── Cached computation ───────────────────────────────────────────────────

    @Cacheable(value = "reporting-service::S5-F10",
               key = "#userId + ':' + #startDate + ':' + #endDate")
    public FinancialHealthScoreDTO computeHealthScore(Long userId,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        // Step c, e — Orchestrate Feign calls
        UserProfileDTO profile;
        try {
            profile = userServiceClient.getUserProfile(userId);
        } catch (FeignException.NotFound e) {
            throw new RuntimeException("User not found with id: " + userId);
        } catch (FeignException e) {
            log.warn("user-service unavailable for user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException("User service temporarily unavailable");
        }
        
        AccountBalanceSummaryDTO accounts;
        try {
            accounts = accountServiceClient.getBalanceSummary(userId);
        } catch (FeignException.NotFound e) {
            accounts = new AccountBalanceSummaryDTO(0, 0.0, null);
        } catch (FeignException e) {
            log.warn("account-service unavailable for user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException("Account service temporarily unavailable");
        }
        
        NetIncomeDTO txn;
        try {
            txn = transactionServiceClient.getUserNetIncome(userId, startDate.toString(), endDate.toString());
        } catch (FeignException.NotFound e) {
            txn = new NetIncomeDTO(0.0, 0, 0.0, 0.0);
        } catch (FeignException e) {
            log.warn("transaction-service unavailable for user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException("Transaction service temporarily unavailable");
        }
        
        BudgetSummaryDTO budgets;
        try {
            budgets = budgetServiceClient.getUserBudgetSummary(userId, startDate.toString(), endDate.toString());
        } catch (FeignException.NotFound e) {
            budgets = new BudgetSummaryDTO(0.0, 0.0, 0.0);
        } catch (FeignException e) {
            log.warn("budget-service unavailable for user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException("Budget service temporarily unavailable");
        }

        // Step e — compute component rates
        double savingsRate = clamp(100.0 * (txn.totalIncome() - txn.totalExpenses()) / Math.max(1, txn.totalIncome()), 0, 100);
        double budgetAdherenceRate = budgets.getWeightedAdherenceRate();
        double goalProgressRate = computeGoalProgress(profile.getFinancialGoals());
        double accountLiquidityRate = computeLiquidity(accounts.getTotalActiveBalance(), txn.totalExpenses(), startDate, endDate);

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

    private double computeGoalProgress(List<FinancialGoalDTO> goals) {
        if (goals == null || goals.isEmpty()) return 0.0;

        double sum = 0.0;
        int activeCount = 0;
        LocalDate today = LocalDate.now();

        for (FinancialGoalDTO goal : goals) {
            if (goal.getDeadline() != null && !goal.getDeadline().isBefore(today) && goal.getCurrentAmount() < goal.getTargetAmount()) {
                activeCount++;
                if (goal.getTargetAmount() > 0) {
                    sum += 100.0 * Math.min(1.0, goal.getCurrentAmount() / goal.getTargetAmount());
                }
            }
        }

        if (activeCount == 0) return 0.0;
        return sum / activeCount;
    }

    private double computeLiquidity(double totalBalance, double totalExpense, LocalDate startDate, LocalDate endDate) {
        if (totalExpense <= 0) return 100.0;
        long daysInRange = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double monthlyAvg = totalExpense * 30.0 / daysInRange;
        double threeMthAvg = 3.0 * monthlyAvg;

        if (threeMthAvg <= 0) return 100.0;
        return clamp(100.0 * Math.min(1.0, totalBalance / threeMthAvg), 0, 100);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

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
