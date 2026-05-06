package com.team31.financetracker.user.service;

import com.team31.financetracker.user.dto.*;
import com.team31.financetracker.user.model.User;
import com.team31.financetracker.user.model.nosql.AuthEvent;
import com.team31.financetracker.user.observer.MongoEventLogger;
import com.team31.financetracker.user.observer.EntityObserver;
import com.team31.financetracker.user.adapter.MongoDocumentAdapter;
import com.team31.financetracker.user.adapter.ObjectArrayDtoAdapter;
import org.springframework.dao.DataIntegrityViolationException;
import com.team31.financetracker.user.repository.UserRepository;
import com.team31.financetracker.user.repository.nosql.AuthEventRepository;
import org.springframework.cache.annotation.Cacheable;
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
import com.team31.financetracker.user.dto.RegisterRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthEventRepository authEventRepository;
    private final ObjectArrayDtoAdapter objectArrayDtoAdapter;
    private final MongoDocumentAdapter mongoDocumentAdapter;
    private final CacheInvalidationService cacheInvalidationService;
    private final List<EntityObserver> observers = new ArrayList<>();

    // 2. Update your Constructor
    public UserService(UserRepository userRepository,
            FinancialGoalRepository financialGoalRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthEventRepository authEventRepository,
            ObjectArrayDtoAdapter objectArrayDtoAdapter,
            MongoDocumentAdapter mongoDocumentAdapter,
            CacheInvalidationService cacheInvalidationService,
            MongoEventLogger mongoEventLogger) { // Add this
        this.userRepository = userRepository;
        this.financialGoalRepository = financialGoalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authEventRepository = authEventRepository;
        this.objectArrayDtoAdapter = objectArrayDtoAdapter;
        this.mongoDocumentAdapter = mongoDocumentAdapter;
        this.cacheInvalidationService = cacheInvalidationService;
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }
    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver obs : observers) {
            obs.onEvent(eventType, payload);
        }
    }

    // Create
    public User createUser(User user) {
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = userRepository.save(user);
            cacheInvalidationService.evictUserFeatureCaches();

            // RETROFIT: Observer notification (Section 4.5 M1 write endpoint)
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", savedUser.getId());
            payload.put("email", savedUser.getEmail());
            notifyObservers("USER_CREATED", payload);

            return savedUser;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or phone already exists");
        }
    }

    // Read All
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Read by ID
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "userDetailCache", key = "'user-service::user::' + #id")
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // Helper: get the currently authenticated user from SecurityContextHolder
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    // Helper: enforce that caller is the owner OR an ADMIN
    private void enforceOwnership(Long targetId) {
        User caller = getAuthenticatedUser();
        if (caller == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
        }
        boolean isAdmin = caller.getRole().name().equals("ADMIN");
        if (!isAdmin && !caller.getId().equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    // Update
    @Transactional
    public User updateUser(Long id, User userDetails) {
        enforceOwnership(id);
        User existingUser = getUserById(id);
        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPhone(userDetails.getPhone());

        if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        existingUser.setRole(userDetails.getRole());
        User saved = userRepository.save(existingUser);
        cacheInvalidationService.evictUserDetail(id);
        cacheInvalidationService.evictUserFeatureCaches();
        return saved;
    }

    // Delete
    @Transactional
    public void deleteUser(Long id) {
        enforceOwnership(id);
        User user = getUserById(id);
        userRepository.delete(user);
        cacheInvalidationService.evictUserDetail(id);
        cacheInvalidationService.evictUserFeatureCaches();
    }

    // Search with Filter (S1-F1)
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "s1f1Cache", key = "'user-service::S1-F1::' + (#name == null ? '' : #name) + ':' + (#email == null ? '' : #email) + ':' + (#role == null ? '' : #role.name())")
    public List<User> searchUsers(String name, String email, Role role) {
        String searchName = (name == null || name.isBlank()) ? null : name;
        String searchEmail = (email == null || email.isBlank()) ? null : email;
        String searchRole = (role == null) ? null : role.name();

        return userRepository.searchUsers(searchName, searchEmail, searchRole);
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
        notifyObservers("USER_UPDATED", payload);
        cacheInvalidationService.evictUserDetail(id);
        cacheInvalidationService.evictUserFeatureCaches();

        return savedUser;
    }

    // Filter Users by Preference (S1-F5)
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "s1f5Cache", key = "'user-service::S1-F5::' + #key + ':' + #value")
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
        notifyObservers("USER_DEACTIVATED", payload);
        cacheInvalidationService.evictUserDetail(id);
        cacheInvalidationService.evictUserFeatureCaches();

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
        User saved = userRepository.save(user);
        cacheInvalidationService.evictUserDetail(userId);
        cacheInvalidationService.evictUserFeatureCaches();
        return saved;
    }

    // Get User Profile with Goals (S1-F8)
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "s1f8Cache", key = "'user-service::S1-F8::' + #id")
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

        return UserProfileDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .preferences(user.getPreferences())
                .financialGoals(goalDTOs)
                .totalGoals(goalDTOs.size())
                .build();
    }

    // Get User Transaction Summary (S1-F3)
    @Cacheable(cacheNames = "s1f3Cache", key = "'user-service::S1-F3::' + #userId")
    public UserTransactionSummaryDTO getUserTransactionSummary(Long userId) {
        User user = getUserById(userId);

        List<Object[]> results = userRepository.getUserTransactionSummary(userId);

        if (results == null || results.isEmpty()) {
            return UserTransactionSummaryDTO.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .totalTransactions(0L)
                    .completedTransactions(0L)
                    .voidedTransactions(0L)
                    .totalIncome(0.0)
                    .totalExpenses(0.0)
                    .build();
        }

        return objectArrayDtoAdapter.adaptToUserTransactionSummary(results.get(0));
    }

    // Top Savers by Net Income (S1-F6)
    @Cacheable(cacheNames = "s1f6Cache", key = "'user-service::S1-F6::' + #startDate + ':' + #endDate + ':' + #limit")
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
                .map(objectArrayDtoAdapter::adaptToTopSaver)
                .toList();
    }

    // Find users by currency preference with minimum completed transactions (S1-F9)
    @Cacheable(cacheNames = "s1f9Cache", key = "'user-service::S1-F9::' + #currency + ':' + #minTransactions")
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

        notifyObservers("REGISTERED", payload);
        cacheInvalidationService.evictUserDetail(savedUser.getId());
        cacheInvalidationService.evictUserFeatureCaches();

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
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", user.getId());
        payload.put("email", user.getEmail());
        notifyObservers("LOGGED_IN", payload);
        
        return token;
    }

    // CC-2 Role Management
    @Transactional
    public User updateUserRole(Long id, String roleName) {
        User user = getUserById(id);
        Role oldRole = user.getRole();
        try {
            user.setRole(Role.valueOf(roleName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName + ". Valid values: PERSONAL, BUSINESS, ADMIN");
        }
        User savedUser = userRepository.save(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", savedUser.getId());
        payload.put("oldRole", oldRole.name());
        payload.put("newRole", savedUser.getRole().name());
        notifyObservers("ROLE_CHANGED", payload);
        cacheInvalidationService.evictRoleChangeCaches(savedUser.getId());
        cacheInvalidationService.evictUserFeatureCaches();

        return savedUser;
    }

    @Cacheable(
            cacheNames = "s1f12Cache",
            key = "'user-service::S1-F12::' + #id + ':' + (#page == null ? 0 : #page) + ':' + (#size == null ? 10 : #size)"
    )
    public UserActivityFeedResponse getUserActivityFeed(Long id, Integer page, Integer size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User caller)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (!caller.getId().equals(id) && caller.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        getUserById(id);

        int resolvedPage = page == null ? 0 : Math.max(page, 0);
        int resolvedSize = size == null ? 10 : Math.min(Math.max(size, 1), 100);

        Page<AuthEvent> eventsPage = authEventRepository.findByUserIdOrderByTimestampDesc(
                id,
                PageRequest.of(resolvedPage, resolvedSize)
        );

        List<ActivityEventDTO> content = eventsPage.getContent().stream()
                .map(mongoDocumentAdapter::adapt)
                .toList();

        return new UserActivityFeedResponse(content, resolvedPage, resolvedSize, eventsPage.getTotalElements());
    }

}