package com.team31.financetracker.budget.controller;

import com.team31.financetracker.contracts.dto.BudgetSummaryDTO;
import com.team31.financetracker.budget.dto.BudgetAlertDTO;
import com.team31.financetracker.budget.dto.BudgetUsageDTO;
import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.budget.model.BudgetPeriod;
import com.team31.financetracker.budget.model.BudgetStatus;
import com.team31.financetracker.budget.model.Category;
import com.team31.financetracker.budget.service.BudgetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    public BudgetController(BudgetService budgetService, org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate) {
        this.budgetService = budgetService;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/test-redis")
    public String testRedis() {
        redisTemplate.opsForValue().set("TEST_KEY", "TEST_VALUE");
        org.springframework.data.redis.connection.RedisConnectionFactory f = redisTemplate.getConnectionFactory();
        String host = "unknown";
        if (f instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory lcf) {
            host = lcf.getHostName() + ":" + lcf.getPort() + " (db: " + lcf.getDatabase() + ")";
        }
        return "Redis value: " + redisTemplate.opsForValue().get("TEST_KEY") + " @ " + host + " | Docker Key: " + redisTemplate.opsForValue().get("DOCKER_KEY");
    }

    // --- Helper methods for safe type conversions ---

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString().trim()); } catch (Exception e) { return null; }
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString().trim()); } catch (Exception e) { return null; }
    }

    private LocalDate toLocalDate(Object val) {
        if (val == null) return null;
        String s = val.toString().trim();
        try { return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s); } catch (Exception e) { return null; }
    }

    private Category toCategory(Object val) {
        if (val == null) return Category.OTHER;
        try { return Category.valueOf(val.toString().trim().toUpperCase()); } catch (Exception e) { return Category.OTHER; }
    }

    private BudgetPeriod toBudgetPeriod(Object val) {
        if (val == null) return BudgetPeriod.MONTHLY;
        try { return BudgetPeriod.valueOf(val.toString().trim().toUpperCase()); } catch (Exception e) { return BudgetPeriod.MONTHLY; }
    }

    private BudgetStatus toBudgetStatus(Object val) {
        if (val == null) return BudgetStatus.ACTIVE;
        try { return BudgetStatus.valueOf(val.toString().trim().toUpperCase()); } catch (Exception e) { return BudgetStatus.ACTIVE; }
    }

    @SuppressWarnings("unchecked")
    private Budget mapToBudget(Map<String, Object> body) {
        Budget budget = new Budget();
        if (body.containsKey("userId")) budget.setUserId(toLong(body.get("userId")));
        if (body.containsKey("category")) budget.setCategory(toCategory(body.get("category")));

        // Accept both "amount" and "budgetAmount"
        if (body.containsKey("amount")) {
            budget.setAmount(toDouble(body.get("amount")));
        } else if (body.containsKey("budgetAmount")) {
            budget.setAmount(toDouble(body.get("budgetAmount")));
        }

        if (body.containsKey("spentAmount")) budget.setSpentAmount(toDouble(body.get("spentAmount")));
        if (body.containsKey("period")) budget.setPeriod(toBudgetPeriod(body.get("period")));
        if (body.containsKey("startDate")) budget.setStartDate(toLocalDate(body.get("startDate")));
        if (body.containsKey("endDate")) budget.setEndDate(toLocalDate(body.get("endDate")));
        if (body.containsKey("status")) budget.setStatus(toBudgetStatus(body.get("status")));

        if (body.containsKey("metadata") && body.get("metadata") instanceof Map) {
            budget.setMetadata((Map<String, Object>) body.get("metadata"));
        }

        if (body.containsKey("createdAt") && body.get("createdAt") != null) {
            try { budget.setCreatedAt(LocalDateTime.parse(body.get("createdAt").toString())); } catch (Exception ignored) {}
        }

        return budget;
    }

    // --- Helper to extract JWT claims from request attributes ---

    private Long getJwtUid(HttpServletRequest request) {
        Object uid = request.getAttribute("jwt.uid");
        if (uid instanceof Long l) return l;
        if (uid instanceof Number n) return n.longValue();
        if (uid != null) {
            try { return Long.parseLong(uid.toString()); } catch (Exception e) { /* ignore */ }
        }
        return null;
    }

    private String getJwtRole(HttpServletRequest request) {
        Object role = request.getAttribute("jwt.role");
        return role != null ? role.toString() : null;
    }

    // ──────────────────── CRUD Endpoints ────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Budget createBudget(@RequestBody Map<String, Object> body) {
        Budget budget = mapToBudget(body);
        return budgetService.createBudget(budget);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createBudgetsBatch(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawBudgets = (List<Map<String, Object>>) body.get("budgets");
        List<Budget> budgets = new ArrayList<>();
        if (rawBudgets != null) {
            for (Map<String, Object> raw : rawBudgets) {
                budgets.add(mapToBudget(raw));
            }
        }
        List<Budget> saved = budgetService.createBudgetsBatch(userId, budgets);
        return Map.of("count", saved.size());
    }

    @GetMapping
    public List<Budget> getAllBudgets() {
        return budgetService.getAllBudgets();
    }

    @GetMapping("/overspent")
    public List<com.team31.financetracker.budget.dto.OverspentBudgetDTO> getOverspentBudgets(
            @RequestParam(required = false, defaultValue = "0") Double minOverspend,
            @RequestParam(required = false, defaultValue = "false") Boolean warningNotSent
    ) {
        return budgetService.getOverspentBudgets(minOverspend, warningNotSent);
    }

    @GetMapping("/{id}")
    public Budget getBudgetById(@PathVariable Long id) {
        return budgetService.getBudgetById(id);
    }

    @PutMapping("/{id}")
    public Budget updateBudget(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Budget updates = mapToBudget(body);
        return budgetService.updateBudget(id, updates);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
    }

    @GetMapping("/user/{userId}/active")
    public Budget getActiveBudgetForUserByCategory(
            @PathVariable Long userId,
            @RequestParam(required = false) String category
    ) {
        if (category == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category missing");
        }
        Category catEnum;
        try {
            catEnum = Category.valueOf(category.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            catEnum = Category.OTHER;
        }

        try {
            return budgetService.getActiveBudgetForUserByCategory(userId, catEnum);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User or budget not found");
        }
    }

    @GetMapping("/user/{userId}/active-count")
    public int getActiveBudgetCount(@PathVariable Long userId) {
        return budgetService.countActiveBudgetsByUser(userId);
    }

    @DeleteMapping("/purge")
    public Integer purgeOldBudgets(@RequestParam int olderThanDays) {
        return budgetService.purgeOldBudgets(olderThanDays);
    }

    @GetMapping("/user/{userId}/summary")
    public BudgetSummaryDTO getBudgetSummary(
            @PathVariable Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDate sDate = null;
        LocalDate eDate = null;
        try {
            if (startDate != null && !startDate.isBlank()) {
                sDate = LocalDate.parse(startDate.trim().length() > 10 ? startDate.trim().substring(0, 10) : startDate.trim());
            }
            if (endDate != null && !endDate.isBlank()) {
                eDate = LocalDate.parse(endDate.trim().length() > 10 ? endDate.trim().substring(0, 10) : endDate.trim());
            }
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format");
        }
        return budgetService.getBudgetSummary(userId, sDate, eDate);
    }

    @GetMapping("/metadata/search")
    public List<Budget> searchBudgetsByMetadata(
            @RequestParam String key,
            @RequestParam String operator,
            @RequestParam String value
    ) {
        return budgetService.searchBudgetsByMetadata(key, operator, value);
    }

    @GetMapping("/history")
    public List<Budget> getBudgetsHistory(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String category
    ) {
        LocalDate sDate;
        LocalDate eDate;
        try {
            sDate = LocalDate.parse(startDate.trim().length() > 10 ? startDate.trim().substring(0, 10) : startDate.trim());
            eDate = LocalDate.parse(endDate.trim().length() > 10 ? endDate.trim().substring(0, 10) : endDate.trim());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format");
        }
        return budgetService.getBudgetsHistory(sDate, eDate, category);
    }

    @PutMapping("/{budgetId}/metadata")
    public Budget updateBudgetMetadata(
            @PathVariable Long budgetId,
            @RequestBody Map<String, Object> metadata
    ) {
        if (metadata == null || metadata.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadata payload cannot be empty");
        }
        return budgetService.updateBudgetMetadata(budgetId, metadata);
    }

    @GetMapping("/near-limit")
    public List<BudgetAlertDTO> getBudgetsNearLimit(
            @RequestParam Double threshold,
            @RequestParam(required = false) String status
    ) {
        BudgetStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = BudgetStatus.valueOf(status.toUpperCase().trim()); } catch (Exception ignored) {}
        }
        return budgetService.getBudgetsNearLimit(threshold, statusEnum);
    }

    @PostMapping("/{id}/usage")
    @ResponseStatus(HttpStatus.CREATED)
    public void recordBudgetUsage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        Double spentAmount = toDouble(body.get("spentAmount"));
        String notes = body.get("notes") != null ? body.get("notes").toString() : null;
        budgetService.recordUsage(id, spentAmount, notes);
    }

    // ──────────────────── S4-F12: Get Budget Usage Timeline ────────────────────

    @GetMapping("/{id}/usage")
    public List<BudgetUsageDTO> getBudgetUsageTimeline(
            @PathVariable Long id,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletRequest request
    ) {
        Long jwtUid = getJwtUid(request);
        String jwtRole = getJwtRole(request);

        Instant start = null;
        Instant end = null;

        if (startTime != null && !startTime.isBlank()) {
            try {
                start = Instant.parse(startTime);
            } catch (Exception e) {
                try {
                    start = LocalDateTime.parse(startTime).toInstant(java.time.ZoneOffset.UTC);
                } catch (Exception ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid startTime format");
                }
            }
        }

        if (endTime != null && !endTime.isBlank()) {
            try {
                end = Instant.parse(endTime);
            } catch (Exception e) {
                try {
                    end = LocalDateTime.parse(endTime).toInstant(java.time.ZoneOffset.UTC);
                } catch (Exception ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid endTime format");
                }
            }
        }

        return budgetService.getBudgetUsageTimeline(id, start, end, jwtUid, jwtRole);
    }
}