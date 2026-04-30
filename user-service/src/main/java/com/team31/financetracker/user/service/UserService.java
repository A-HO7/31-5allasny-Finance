package com.team31.financetracker.user.service;

import com.team31.financetracker.user.dto.*;
import com.team31.financetracker.user.model.User;
import com.team31.financetracker.user.observer.MongoEventLogger;
import org.springframework.dao.DataIntegrityViolationException;
import com.team31.financetracker.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.team31.financetracker.user.model.Role;
import com.team31.financetracker.user.model.UserStatus;
import org.springframework.transaction.annotation.Transactional;
import com.team31.financetracker.user.repository.FinancialGoalRepository;
import com.team31.financetracker.user.model.FinancialGoal;

import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.team31.financetracker.user.service.JwtService;
import com.team31.financetracker.user.dto.RegisterRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MongoEventLogger mongoEventLogger;

    // 2. Update your Constructor
    public UserService(UserRepository userRepository,
            FinancialGoalRepository financialGoalRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            MongoEventLogger mongoEventLogger) { // Add this
        this.userRepository = userRepository;
        this.financialGoalRepository = financialGoalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mongoEventLogger = mongoEventLogger; // Add this
    }

    // Create
    public User createUser(User user) {
        try {

            user.setPassword(passwordEncoder.encode(user.getPassword()));

            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or phone already exists");
        }
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

        if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        existingUser.setRole(userDetails.getRole());
        return userRepository.save(existingUser);
    }

    // Delete
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    // Search with Filter (S1-F1)
    public List<User> searchUsers(String name, String email, Role role) {
        return userRepository.searchUsers(name, email, role != null ? role.name() : null);
    }

    // Update Preferences (S1-F2)
    public User updatePreferences(Long id, Map<String, Object> newPreferences) {
        User user = getUserById(id);
        Map<String, Object> existing = user.getPreferences();
        if (existing == null) {
            user.setPreferences(newPreferences);
        } else {
            existing.putAll(newPreferences); // merges, overwrites same keys, adds new keys
            user.setPreferences(existing);
        }
        User savedUser = userRepository.save(user);

        // RETROFIT: Observer notification (M2 Requirement Section 4.5)
        Map<String, Object> payload = new HashMap<>(newPreferences);
        payload.put("userId", savedUser.getId());
        payload.put("action", "USER_UPDATED");
        mongoEventLogger.onEvent("USER_UPDATED", payload);

        return savedUser;
    }

    // Filter Users by Preference (S1-F5)
    public List<User> filterByPreference(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key and value must not be blank");
        }
        return userRepository.findByPreference(key, value);
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
        User savedUser = userRepository.save(user);

        // RETROFIT: Observer notification
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", savedUser.getId());
        payload.put("action", "USER_DEACTIVATED");
        mongoEventLogger.onEvent("USER_DEACTIVATED", payload);

        return savedUser;
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

    // Get User Profile with Goals (S1-F8)
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfileWithGoals(Long id) {
        User user = getUserById(id);

        List<FinancialGoalDTO> goalDTOs = user.getFinancialGoals().stream()
                .map(goal -> new FinancialGoalDTO(
                        goal.getLabel(),
                        goal.getTargetAmount(),
                        goal.getCurrentAmount(),
                        goal.getDeadline(),
                        goal.getPrimary(),
                        goal.getMetadata()))
                .collect(Collectors.toList());

        return new UserProfileDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getPreferences(),
                goalDTOs,
                goalDTOs.size());
    }

    // Get User Transaction Summary (S1-F3)
    public UserTransactionSummaryDTO getUserTransactionSummary(Long userId) {
        User user = getUserById(userId);

        List<Object[]> results = userRepository.getUserTransactionSummary(userId);

        if (results == null || results.isEmpty()) {
            return new UserTransactionSummaryDTO(
                    user.getId(), user.getName(), 0L, 0L, 0L, 0.0, 0.0);
        }

        Object[] result = results.get(0);

        return new UserTransactionSummaryDTO(
                ((Number) result[0]).longValue(),
                (String) result[1],
                ((Number) result[2]).longValue(),
                ((Number) result[3]).longValue(),
                ((Number) result[4]).longValue(),
                ((Number) result[5]).doubleValue(),
                ((Number) result[6]).doubleValue());
    }

    // Top Savers by Net Income (S1-F6)
    public List<TopSaverDTO> getTopSaversByNetIncome(LocalDate startDate, LocalDate endDate, int limit) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }

        if (limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be greater than 0");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Object[]> results = userRepository.getTopSaversByNetIncome(startDateTime, endDateTime, limit);

        return results.stream()
                .map(result -> new TopSaverDTO(
                        ((Number) result[0]).longValue(),
                        (String) result[1],
                        ((Number) result[2]).doubleValue(),
                        ((Number) result[3]).longValue()))
                .toList();
    }

    // Find users by currency preference with minimum completed transactions (S1-F9)
    public List<CurrencyPreferenceUserDTO> findUsersByCurrencyPreference(String currency, int minTransactions) {
        if (currency == null || currency.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency must not be blank");
        }

        String trimmed = currency.trim();
        List<Object[]> rows = userRepository.findUsersByCurrencyPreferenceAndMinCompletedTransactions(
                trimmed, minTransactions);

        return rows.stream()
                .map(row -> new CurrencyPreferenceUserDTO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue()))
                .toList();
    }

    // Register User (S1-F10)
    public String registerUser(RegisterRequest request) {
        // a) Validate not blank
        if (request.name() == null || request.name().isBlank() ||
                request.email() == null || request.email().isBlank() ||
                request.password() == null || request.password().isBlank() ||
                request.phone() == null || request.phone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All fields are required");
        }

        // b) Check if already exists (Conflict 409)
        if (userRepository.existsByEmail(request.email()) || userRepository.existsByPhone(request.phone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or phone already registered");
        }

        // c) Create & Map User
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password())); // HASHING!
        user.setPhone(request.phone());
        user.setRole(Role.PERSONAL); // Always default to PERSONAL
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", savedUser.getId());
        payload.put("email", savedUser.getEmail());

        mongoEventLogger.onEvent("REGISTERED", payload);

        return jwtService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
    }

    // Login User (S1-F11)
    public String loginUser(String email, String password) {
        // 1. Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // 2. Verify password (matches raw input against hashed DB value)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // 3. Generate and return token
        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    // CC-2 Role Management
    @Transactional
    public User updateUserRole(Long id, String roleName) {
        User user = getUserById(id);
        // This will throw IllegalArgumentException if the string is not a valid enum
        // value
        user.setRole(Role.valueOf(roleName.toUpperCase()));
        return userRepository.save(user);
    }

}