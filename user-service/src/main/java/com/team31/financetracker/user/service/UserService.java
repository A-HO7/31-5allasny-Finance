package com.team31.financetracker.user.service;

import com.team31.financetracker.contracts.dto.NetIncomeDTO;
import com.team31.financetracker.contracts.dto.UserTransactionSummaryDTO;
import com.team31.financetracker.contracts.events.UserDeactivatedEvent;
import com.team31.financetracker.contracts.events.UserRegisteredEvent;
import com.team31.financetracker.user.dto.ActivityEventDTO;
import com.team31.financetracker.user.dto.FinancialGoalDTO;
import com.team31.financetracker.user.dto.RegisterRequest;
import com.team31.financetracker.user.dto.TopSaverDTO;
import com.team31.financetracker.user.dto.UserActivityFeedResponse;
import com.team31.financetracker.user.dto.UserProfileDTO;
import com.team31.financetracker.user.messaging.publishers.UserEventPublisher;
import com.team31.financetracker.user.exception.ServiceUnavailableException;
import com.team31.financetracker.user.model.User;
import com.team31.financetracker.user.model.nosql.AuthEvent;
import com.team31.financetracker.user.observer.MongoEventLogger;
import com.team31.financetracker.user.observer.EntityObserver;
import com.team31.financetracker.user.adapter.MongoDocumentAdapter;
import feign.FeignException;
import org.slf4j.MDC;
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
import com.team31.financetracker.contracts.feign.BudgetServiceClient;
import com.team31.financetracker.contracts.feign.TransactionServiceClient;

import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.ArrayList;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthEventRepository authEventRepository;
    private final TransactionServiceClient transactionServiceClient;
    private final BudgetServiceClient budgetServiceClient;
    private final UserEventPublisher userEventPublisher;
    private final MongoDocumentAdapter mongoDocumentAdapter;
    private final CacheInvalidationService cacheInvalidationService;
    private final List<EntityObserver> observers = new ArrayList<>();

    public UserService(UserRepository userRepository,
            FinancialGoalRepository financialGoalRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthEventRepository authEventRepository,
            MongoDocumentAdapter mongoDocumentAdapter,
            CacheInvalidationService cacheInvalidationService,
            TransactionServiceClient transactionServiceClient,
            BudgetServiceClient budgetServiceClient,
            UserEventPublisher userEventPublisher,
            MongoEventLogger mongoEventLogger) {
        this.userRepository = userRepository;
        this.financialGoalRepository = financialGoalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authEventRepository = authEventRepository;
        this.mongoDocumentAdapter = mongoDocumentAdapter;
        this.cacheInvalidationService = cacheInvalidationService;
        this.transactionServiceClient = transactionServiceClient;
        this.budgetServiceClient = budgetServiceClient;
        this.userEventPublisher = userEventPublisher;
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

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
        boolean setUserId = MDC.get("userId") == null;
        if (setUserId) {
            MDC.put("userId", id.toString());
        }
        try {
            validateOwnershipOrAdmin(id);
            return userRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        } finally {
            if (setUserId) {
                MDC.remove("userId");
            }
        }
    }

    // Update
    @Transactional
    public User updateUser(Long id, User userDetails) {
        MDC.put("userId", id.toString());
        try {
        validateOwnershipOrAdmin(id);

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPhone(userDetails.getPhone());

        if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        // Role Change Protection: Only an ADMIN can change roles
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User caller) {
            if (caller.getRole() == Role.ADMIN) {
                existingUser.setRole(userDetails.getRole());
            }
        }

        User saved = userRepository.save(existingUser);
        cacheInvalidationService.evictUserDetail(id);
        cacheInvalidationService.evictUserFeatureCaches();
        return saved;
        } finally {
            MDC.remove("userId");
        }
    }

    // Delete
    @Transactional
    public void deleteUser(Long id) {
        MDC.put("userId", id.toString());
        try {
        validateOwnershipOrAdmin(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userRepository.delete(user);
        cacheInvalidationService.evictUserDetail(id);
        cacheInvalidationService.evictUserFeatureCaches();
        } finally {
            MDC.remove("userId");
        }
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
        MDC.put("userId", id.toString());
        try {
        // 1. Security Check
        validateOwnershipOrAdmin(id);

        // 2. Fetch User
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 3. Merge Logic (M1 Requirement)
        Map<String, Object> existing = user.getPreferences();
        if (existing == null) {
            user.setPreferences(newPreferences);
        } else {
            existing.putAll(newPreferences); // merges, overwrites same keys
            user.setPreferences(existing);
        }

        // 4. Save
        User savedUser = userRepository.save(user);

        // 5. Notify Observers (M2 Retrofit)
        Map<String, Object> payload = new HashMap<>(newPreferences);
        payload.put("userId", savedUser.getId());
        payload.put("action", "USER_UPDATED");
        notifyObservers("USER_UPDATED", payload);

        // 6. Cache Invalidation
        cacheInvalidationService.evictUserDetail(id);
        cacheInvalidationService.evictUserFeatureCaches();

        return savedUser;
        } finally {
            MDC.remove("userId");
        }
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
        MDC.put("userId", id.toString());
        try {
        validateOwnershipOrAdmin(id);

        int activeBudgets;
        try {
            activeBudgets = budgetServiceClient.getActiveBudgetCount(id);
        } catch (FeignException.NotFound e) {
            activeBudgets = 0;
        } catch (FeignException e) {
            throw new ServiceUnavailableException("Downstream service temporarily unavailable");
        }

        if (activeBudgets > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has active budgets");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setStatus(UserStatus.DEACTIVATED);
        User savedUser = userRepository.save(user);

        userEventPublisher.publishUserDeactivated(new UserDeactivatedEvent(id));

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", savedUser.getId());
        payload.put("action", "USER_DEACTIVATED");
        notifyObservers("USER_DEACTIVATED", payload);
        cacheInvalidationService.evictUserDetail(id);
        cacheInvalidationService.evictUserFeatureCaches();

        return savedUser;
        } finally {
            MDC.remove("userId");
        }
    }

    // Set Primary Financial Goal (S1-F7)
    @Transactional
    public User setPrimaryFinancialGoal(Long userId, Long goalId) {
        MDC.put("userId", userId.toString());
        try {
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
        } finally {
            MDC.remove("userId");
        }
    }

    // Get User Profile with Goals (S1-F8)
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "user-service", key = "'user-service::S1-F8::' + #id")
    public UserProfileDTO getUserProfileWithGoals(Long id) {
        MDC.put("userId", id.toString());
        try {
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
        } finally {
            MDC.remove("userId");
        }
    }

    // Get User Transaction Summary (S1-F3)
    @Cacheable(cacheNames = "user-service", key = "'user-service::S1-F3::' + #userId")
    @Transactional(readOnly = true)
    public UserTransactionSummaryDTO getUserTransactionSummary(Long userId) {
        MDC.put("userId", userId.toString());
        try {
        User user = getUserById(userId);

        UserTransactionSummaryDTO remote;
        try {
            remote = transactionServiceClient.getUserTransactionSummary(userId);
        } catch (FeignException.NotFound e) {
            remote = null;
        } catch (FeignException e) {
            throw new ServiceUnavailableException("Downstream service temporarily unavailable");
        }

        if (remote == null) {
            return emptyUserTransactionSummary(user);
        }

        return UserTransactionSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalTransactions(longOrZero(remote.getTotalTransactions()))
                .completedTransactions(longOrZero(remote.getCompletedTransactions()))
                .voidedTransactions(longOrZero(remote.getVoidedTransactions()))
                .totalIncome(remote.getTotalIncome() != null ? remote.getTotalIncome() : 0.0)
                .totalExpenses(remote.getTotalExpenses() != null ? remote.getTotalExpenses() : 0.0)
                .build();
        } finally {
            MDC.remove("userId");
        }
    }

    // Top Savers by Net Income (S1-F6)
    @Cacheable(cacheNames = "user-service", key = "'user-service::S1-F6::' + #startDate + ':' + #endDate + ':' + #limit")
    @Transactional(readOnly = true)
    public List<TopSaverDTO> getTopSaversByNetIncome(LocalDate startDate, LocalDate endDate, int limit) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }

        if (limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be greater than 0");
        }

        String startDateStr = startDate.toString();
        String endDateStr = endDate.toString();

        List<TopSaverDTO> aggregated = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            NetIncomeDTO netIncomeDTO = fetchUserNetIncomeOrZero(u.getId(), startDateStr, endDateStr);
            double savings = netIncomeDTO.netSavings() != null ? netIncomeDTO.netSavings() : 0.0;
            aggregated.add(TopSaverDTO.builder()
                    .userId(u.getId())
                    .name(u.getName())
                    .netSavings(savings)
                    .transactionCount((long) netIncomeDTO.transactionCount())
                    .build());
        }

        aggregated.sort(Comparator.comparing(TopSaverDTO::getNetSavings).reversed());
        return aggregated.stream().limit(limit).toList();
    }

    // Find users by currency preference with minimum completed transactions (S1-F9)
    @Cacheable(cacheNames = "user-service", key = "'user-service::S1-F9::' + #currency + ':' + #minTransactions")
    @Transactional(readOnly = true)
    public List<User> findUsersByCurrencyPreference(String currency, int minTransactions) {
        if (currency == null || currency.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency must not be blank");
        }

        String trimmed = currency.trim();
        List<User> matched = userRepository.findUsersWithDefaultCurrency(trimmed);
        List<User> result = new ArrayList<>();

        for (User u : matched) {
            long count = fetchCompletedTransactionCountOrZero(u.getId());
            if (count >= minTransactions) {
                result.add(u);
            }
        }
        return result;
    }

    private UserTransactionSummaryDTO emptyUserTransactionSummary(User user) {
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

    private long longOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private NetIncomeDTO fetchUserNetIncomeOrZero(Long userId, String startDateStr, String endDateStr) {
        try {
            NetIncomeDTO dto = transactionServiceClient.getUserNetIncome(userId, startDateStr, endDateStr);
            return dto != null ? dto : emptyNetIncome();
        } catch (FeignException.NotFound e) {
            return emptyNetIncome();
        } catch (FeignException e) {
            return emptyNetIncome();
        }
    }

    private static NetIncomeDTO emptyNetIncome() {
        return new NetIncomeDTO(0.0, 0, 0.0, 0.0);
    }

    private long fetchCompletedTransactionCountOrZero(Long userId) {
        try {
            return transactionServiceClient.getCompletedTransactionCount(userId);
        } catch (FeignException.NotFound e) {
            return 0L;
        } catch (FeignException e) {
            return 0L;
        }
    }

    // Register User (S1-F10)
    @Transactional
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

        userEventPublisher.publishUserRegistered(new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        ));

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
        MDC.put("userId", id.toString());
        try {
        User user = getUserById(id);
        Role oldRole = user.getRole();
        try {
            user.setRole(Role.valueOf(roleName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role: " + roleName + ". Valid values: PERSONAL, BUSINESS, ADMIN");
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
        } finally {
            MDC.remove("userId");
        }
    }

    @Cacheable(cacheNames = "user-service", key = "'user-service::S1-F12::' + #id + ':' + (#page == null ? 0 : #page) + ':' + (#size == null ? 10 : #size)")
    public UserActivityFeedResponse getUserActivityFeed(Long id, Integer page, Integer size) {
        MDC.put("userId", id.toString());
        try {
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
                PageRequest.of(resolvedPage, resolvedSize));

        List<ActivityEventDTO> content = eventsPage.getContent().stream()
                .map(mongoDocumentAdapter::adapt)
                .toList();

        return new UserActivityFeedResponse(content, resolvedPage, resolvedSize, eventsPage.getTotalElements());
        } finally {
            MDC.remove("userId");
        }
    }

    private void validateOwnershipOrAdmin(Long targetUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // If not authenticated at all
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        // Ensure the principal is our User model
        if (!(auth.getPrincipal() instanceof User caller)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }

        // IDOR Check: Must be owner OR Admin
        boolean isOwner = caller.getId().equals(targetUserId);
        boolean isAdmin = caller.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this resource");
        }
    }

}