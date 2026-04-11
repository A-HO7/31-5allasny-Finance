package com.team31.financetracker.user.service;

import com.team31.financetracker.user.model.FinancialGoal;
import com.team31.financetracker.user.model.User;
import com.team31.financetracker.user.repository.FinancialGoalRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FinancialGoalService {

    private final FinancialGoalRepository goalRepository;
    private final UserService userService;

    public FinancialGoalService(FinancialGoalRepository goalRepository, UserService userService) {
        this.goalRepository = goalRepository;
        this.userService = userService;
    }

    // Create (Must link to an existing User)
    public FinancialGoal createGoal(Long userId, FinancialGoal goal) {
        User user = userService.getUserById(userId);
        goal.setUser(user);
        try {
            return goalRepository.save(goal);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not create goal: " + e.getMessage());
        }
    }

    // Read All goals by User ID
    public List<FinancialGoal> getGoalsByUserId(Long userId) {
        userService.getUserById(userId); // throws 404 if user doesn't exist
        return goalRepository.findByUserId(userId);
    }

    // Read All
    public List<FinancialGoal> getAllGoals() {
        return goalRepository.findAll();
    }

    // Read by ID
    public FinancialGoal getGoalById(Long id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));
    }

    // Update
    public FinancialGoal updateGoal(Long id, FinancialGoal goalDetails) {
        FinancialGoal existingGoal = getGoalById(id);
        existingGoal.setLabel(goalDetails.getLabel());
        existingGoal.setTargetAmount(goalDetails.getTargetAmount());
        existingGoal.setCurrentAmount(goalDetails.getCurrentAmount());
        existingGoal.setDeadline(goalDetails.getDeadline());
        existingGoal.setPrimary(goalDetails.getPrimary());
        existingGoal.setMetadata(goalDetails.getMetadata());
        return goalRepository.save(existingGoal);
    }

    // Delete
    public void deleteGoal(Long id) {
        FinancialGoal goal = getGoalById(id);
        goalRepository.delete(goal);
    }
}