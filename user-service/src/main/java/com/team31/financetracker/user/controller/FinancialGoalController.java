package com.team31.financetracker.user.controller;

import com.team31.financetracker.user.model.FinancialGoal;
import com.team31.financetracker.user.service.FinancialGoalService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FinancialGoalController {

    private final FinancialGoalService goalService;

    public FinancialGoalController(FinancialGoalService goalService) {
        this.goalService = goalService;
    }

    // CREATE
    @PostMapping("/api/goals/user/{userId}")
    public FinancialGoal createGoal(@PathVariable Long userId, @RequestBody FinancialGoal goal) {
        return goalService.createGoal(userId, goal);
    }

    // CREATE
    @PostMapping("/api/users/{userId}/goals")
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialGoal createGoalForUser(@PathVariable Long userId, @RequestBody FinancialGoal goal) {
        return goalService.createGoal(userId, goal);
    }

    // READ ALL goals
    @GetMapping("/api/goals")
    public List<FinancialGoal> getAllGoals() {
        return goalService.getAllGoals();
    }

    // READ ALL goals by user
    @GetMapping("/api/users/{userId}/goals")
    public List<FinancialGoal> getGoalsByUser(@PathVariable Long userId) {
        return goalService.getGoalsByUserId(userId);
    }

    // READ BY ID
    @GetMapping("/api/goals/{id}")
    public FinancialGoal getGoalById(@PathVariable Long id) {
        return goalService.getGoalById(id);
    }

    // UPDATE
    @PutMapping("/api/goals/{id}")
    public FinancialGoal updateGoal(@PathVariable Long id, @RequestBody FinancialGoal goal) {
        return goalService.updateGoal(id, goal);
    }

    // DELETE
    @DeleteMapping("/api/goals/{id}")
    public void deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
    }

    // DELETE
    @DeleteMapping("/api/users/{userId}/goals/{goalId}")
    public void deleteGoalForUser(@PathVariable Long userId, @PathVariable Long goalId) {
        goalService.deleteGoal(goalId);
    }
}