package com.team31.financetracker.user.controller;

import com.team31.financetracker.user.model.FinancialGoal;
import com.team31.financetracker.user.service.FinancialGoalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class FinancialGoalController {

    private final FinancialGoalService goalService;

    public FinancialGoalController(FinancialGoalService goalService) {
        this.goalService = goalService;
    }

    // CREATE (Linked to User ID)
    @PostMapping("/user/{userId}")
    public FinancialGoal createGoal(@PathVariable Long userId, @RequestBody FinancialGoal goal) {
        return goalService.createGoal(userId, goal);
    }

    // READ ALL
    @GetMapping
    public List<FinancialGoal> getAllGoals() {
        return goalService.getAllGoals();
    }

    // READ BY ID
    @GetMapping("/{id}")
    public FinancialGoal getGoalById(@PathVariable Long id) {
        return goalService.getGoalById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public FinancialGoal updateGoal(@PathVariable Long id, @RequestBody FinancialGoal goal) {
        return goalService.updateGoal(id, goal);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
    }
}