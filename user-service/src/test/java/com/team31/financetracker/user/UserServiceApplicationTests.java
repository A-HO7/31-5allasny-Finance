package com.team31.financetracker.user;

import com.team31.financetracker.user.adapter.MongoDocumentAdapter;
import com.team31.financetracker.user.adapter.ObjectArrayDtoAdapter;
import com.team31.financetracker.user.dto.UserActivityFeedResponse;
import com.team31.financetracker.user.model.Role;
import com.team31.financetracker.user.model.User;
import com.team31.financetracker.user.model.nosql.AuthEvent;
import com.team31.financetracker.user.observer.MongoEventLogger;
import com.team31.financetracker.user.repository.FinancialGoalRepository;
import com.team31.financetracker.user.repository.UserRepository;
import com.team31.financetracker.user.repository.nosql.AuthEventRepository;
import com.team31.financetracker.user.service.CacheInvalidationService;
import com.team31.financetracker.user.service.JwtService;
import com.team31.financetracker.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceApplicationTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FinancialGoalRepository financialGoalRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthEventRepository authEventRepository;
    @Mock
    private CacheInvalidationService cacheInvalidationService;
    @Mock
    private MongoEventLogger mongoEventLogger;
    @Mock
    private com.team31.financetracker.contracts.feign.TransactionServiceClient transactionServiceClient;
    @Mock
    private com.team31.financetracker.contracts.feign.BudgetServiceClient budgetServiceClient;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void ownerCanReadActivityFeed() {
        User caller = new User();
        caller.setId(10L);
        caller.setRole(Role.PERSONAL);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caller, null, List.of())
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(caller));

        AuthEvent event = new AuthEvent(10L, "LOGGED_IN", Map.of("email", "a@example.com"));
        event.setTimestamp(LocalDateTime.now());
        when(authEventRepository.findByUserIdOrderByTimestampDesc(eq(10L), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(event)));

        UserService service = new UserService(
                userRepository,
                financialGoalRepository,
                passwordEncoder,
                jwtService,
                authEventRepository,
                new ObjectArrayDtoAdapter(),
                new MongoDocumentAdapter(),
                cacheInvalidationService,
                mongoEventLogger,
                transactionServiceClient,
                budgetServiceClient
        );

        UserActivityFeedResponse response = service.getUserActivityFeed(10L, null, null);
        assertEquals(1, response.content().size());
        assertEquals("LOGGED_IN", response.content().get(0).action());
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonOwnerAndNonAdminGets403OnActivityFeed() {
        User caller = new User();
        caller.setId(11L);
        caller.setRole(Role.PERSONAL);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caller, null, List.of())
        );

        UserService service = new UserService(
                userRepository,
                financialGoalRepository,
                passwordEncoder,
                jwtService,
                authEventRepository,
                new ObjectArrayDtoAdapter(),
                new MongoDocumentAdapter(),
                cacheInvalidationService,
                mongoEventLogger,
                transactionServiceClient,
                budgetServiceClient
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getUserActivityFeed(10L, 0, 10)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        SecurityContextHolder.clearContext();
    }
}
