package com.team31.financetracker.user.controller;

import com.team31.financetracker.user.dto.CurrencyPreferenceUserDTO;
import com.team31.financetracker.user.dto.TopSaverDTO;
import com.team31.financetracker.user.dto.UserTransactionSummaryDTO;
import com.team31.financetracker.user.model.Role;
import com.team31.financetracker.user.model.User;
import com.team31.financetracker.user.service.UserService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import com.team31.financetracker.user.dto.UserProfileDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // CREATE
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    // READ ALL
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // READ BY ID
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
    //Search with Filter (S1-F1)
    @GetMapping("/search")
    public List<User> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role) {

        return userService.searchUsers(name, email, role);
    }
    //Update Preferences (S1-F2)
    @PutMapping("/{id}/preferences")
    public User updatePreferences(@PathVariable Long id, @RequestBody Map<String, Object> preferences) {
        return userService.updatePreferences(id, preferences);
    }
    //Filter Users by Preference (S1-F5)
    @GetMapping("/preferences/search")
    public List<User> filterByPreference(
            @RequestParam String key,
            @RequestParam String value) {
        return userService.filterByPreference(key, value);
    }

    // Deactivate User (S1-F4)
    @PutMapping("/{id}/deactivate")
    public User deactivateUser(@PathVariable Long id) {
        return userService.deactivateUser(id);
    }

    // Set Primary Financial Goal (S1-F7)
    @PutMapping("/{userId}/goals/{goalId}/primary")
    public User setPrimaryFinancialGoal(@PathVariable Long userId, @PathVariable Long goalId) {
        return userService.setPrimaryFinancialGoal(userId, goalId);
    }

    // Get User Profile with Goals (S1-F8)
    @GetMapping("/{id}/profile")
    public UserProfileDTO getUserProfile(@PathVariable Long id) {
        return userService.getUserProfileWithGoals(id);
    }

     // Get User Transaction Summary (S1-F3)
     @GetMapping("/{id}/transaction-summary")
     public UserTransactionSummaryDTO getUserTransactionSummary(@PathVariable Long id) {
         return userService.getUserTransactionSummary(id);
     }


     // Top Savers by Net Income (S1-F6)
    @GetMapping("/reports/top-savers")
    public List<TopSaverDTO> getTopSaversByNetIncome(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam int limit) {
        return userService.getTopSaversByNetIncome(startDate, endDate, limit);
    }

    // Find users by currency preference with minimum completed transactions (S1-F9)
    @GetMapping("/preferences/currency")
    public List<CurrencyPreferenceUserDTO> findUsersByCurrencyPreference(
            @RequestParam String currency,
            @RequestParam int minTransactions) {
        return userService.findUsersByCurrencyPreference(currency, minTransactions);
    }
}


