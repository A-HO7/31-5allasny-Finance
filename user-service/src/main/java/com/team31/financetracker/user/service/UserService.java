package com.team31.financetracker.user.service;

import com.team31.financetracker.user.model.User;
import com.team31.financetracker.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.team31.financetracker.user.model.Role;
import com.team31.financetracker.user.model.UserStatus;
import org.springframework.transaction.annotation.Transactional;
import com.team31.financetracker.user.repository.FinancialGoalRepository;
import com.team31.financetracker.user.model.FinancialGoal;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final FinancialGoalRepository financialGoalRepository;

    public UserService(UserRepository userRepository, FinancialGoalRepository financialGoalRepository) {
        this.userRepository = userRepository;
        this.financialGoalRepository = financialGoalRepository;
    }

    // Create
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // Read All
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Read by ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // Update
    public User updateUser(Long id, User userDetails) {
        User existingUser = getUserById(id);
        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPhone(userDetails.getPhone());
        existingUser.setPassword(userDetails.getPassword());
        existingUser.setRole(userDetails.getRole());
        return userRepository.save(existingUser);
    }

    // Delete
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    //Search with Filter (S1-F1)
    public List<User> searchUsers(String name, String email, Role role) {
        return userRepository.searchUsers(name, email, role != null ? role.name() : null);
    }

    // Deactivate User (S1-F4)
    @Transactional
    public User deactivateUser(Long id) {
        User user = getUserById(id);

        int activeBudgets = userRepository.countActiveBudgetsNative(id);
        if (activeBudgets > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate user with active budgets");
        }

        userRepository.voidPendingTransactionsNative(id);

        user.setStatus(UserStatus.DEACTIVATED);
        return userRepository.save(user);
    }

    // Set Primary Financial Goal (S1-F7)
    @Transactional
    public User setPrimaryFinancialGoal(Long userId, Long goalId) {
        User user = getUserById(userId);

        FinancialGoal goal = financialGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Financial goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Goal does not belong to the user");
        }

        // Remove primary from all user's goals
        for (FinancialGoal g : user.getFinancialGoals()) {
            g.setPrimary(false);
        }

        // Set the target goal to primary
        goal.setPrimary(true);

        // Save via JPA cascade from User
        return userRepository.save(user);
    }
}