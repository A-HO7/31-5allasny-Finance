package com.team31.financetracker.user.service;

import com.team31.financetracker.user.model.FinancialGoal;
import com.team31.financetracker.user.model.User;
import com.team31.financetracker.user.repository.FinancialGoalRepository;
import com.team31.financetracker.user.observer.MongoEventLogger;
import com.team31.financetracker.user.observer.EntityObserver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Service
public class FinancialGoalService {

    private final FinancialGoalRepository goalRepository;
    private final UserService userService;
    private final List<EntityObserver> observers = new ArrayList<>();

    public FinancialGoalService(FinancialGoalRepository goalRepository, UserService userService, MongoEventLogger mongoEventLogger) {
        this.goalRepository = goalRepository;
        this.userService = userService;
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }
    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver obs : observers) {
            obs.onEvent(eventType, payload);
        }
    }

    // Create (Must link to an existing User)
    public FinancialGoal createGoal(Long userId, FinancialGoal goal) {
        User user = userService.getUserById(userId);
        goal.setUser(user);
        try {
            FinancialGoal savedGoal = goalRepository.save(goal);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("goalId", savedGoal.getId());
            notifyObservers("GOAL_CREATED", payload);
            
            return savedGoal;
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
        
        FinancialGoal savedGoal = goalRepository.save(existingGoal);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", savedGoal.getUser().getId());
        payload.put("goalId", savedGoal.getId());
        notifyObservers("GOAL_UPDATED", payload);
        
        return savedGoal;
    }

    // Delete
    public void deleteGoal(Long id) {
        FinancialGoal goal = getGoalById(id);
        goalRepository.delete(goal);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", goal.getUser().getId());
        payload.put("goalId", goal.getId());
        notifyObservers("GOAL_DELETED", payload);
    }
}