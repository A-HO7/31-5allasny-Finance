package com.team31.financetracker.transaction.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team31.financetracker.contracts.dto.AccountDTO;
import com.team31.financetracker.contracts.dto.BudgetDTO;
import com.team31.financetracker.contracts.dto.UserDTO;
import com.team31.financetracker.contracts.events.TransactionCompletedEvent;
import com.team31.financetracker.contracts.feign.AccountServiceClient;
import com.team31.financetracker.contracts.feign.BudgetServiceClient;
import com.team31.financetracker.contracts.feign.UserServiceClient;
import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.observer.MongoEventLogger;
import com.team31.financetracker.transaction.outbox.OutboxEvent;
import com.team31.financetracker.transaction.outbox.OutboxRepository;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import com.team31.financetracker.transaction.service.CacheInvalidationService;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class SagaTriggerService {

    private final TransactionRepository transactionRepository;
    private final UserServiceClient userServiceClient;
    private final AccountServiceClient accountServiceClient;
    private final BudgetServiceClient budgetServiceClient;
    private final OutboxRepository outboxRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final MongoEventLogger mongoEventLogger;
    private final ObjectMapper objectMapper;

    public SagaTriggerService(TransactionRepository transactionRepository,
            UserServiceClient userServiceClient,
            AccountServiceClient accountServiceClient,
            BudgetServiceClient budgetServiceClient,
            OutboxRepository outboxRepository,
            CacheInvalidationService cacheInvalidationService,
            MongoEventLogger mongoEventLogger,
            ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.userServiceClient = userServiceClient;
        this.accountServiceClient = accountServiceClient;
        this.budgetServiceClient = budgetServiceClient;
        this.outboxRepository = outboxRepository;
        this.cacheInvalidationService = cacheInvalidationService;
        this.mongoEventLogger = mongoEventLogger;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Transaction completeTransaction(Long id) {
        Transaction transaction = getTransactionById(id);

        if (transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only APPROVED transactions can be completed");
        }

        Long userId = transaction.getUserId();

        try {
            UserDTO user = userServiceClient.getUser(userId);
            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User must be ACTIVE");
            }
        } catch (feign.FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User must be ACTIVE");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not verify user");
        }

        try {
            AccountDTO account = accountServiceClient.getAccount(transaction.getAccountId());
            if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Account must be ACTIVE");
            }
        } catch (feign.FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account must be ACTIVE");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not verify account");
        }

        String category = transaction.getCategory() != null ? transaction.getCategory().name() : null;
        try {
            BudgetDTO budget = budgetServiceClient.getActiveBudgetForUser(userId, category);
            if (budget != null && budget.getStatus() != null
                    && !"ACTIVE".equalsIgnoreCase(budget.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Budget must be ACTIVE");
            }
        } catch (feign.FeignException.NotFound e) {
            // No active budget is acceptable; continue with completion.
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Could not verify budget");
        }

        transaction.setStatus(TransactionStatus.COMPLETING);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        try {
            String payload = objectMapper.writeValueAsString(
                    new TransactionCompletedEvent(
                            transaction.getId(),
                            transaction.getUserId(),
                            transaction.getAccountId(),
                            transaction.getCategory() != null ? transaction.getCategory().name() : null,
                            transaction.getType().name(),
                            transaction.getAmount(),
                            transaction.getCompletedAt()));
            outboxRepository.save(new OutboxEvent(
                    "transaction.events",
                    "transaction.completed",
                    payload));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not queue completion event");
        }

        mongoEventLogger.onEvent("COMPLETING", transaction);
        cacheInvalidationService.evictAllTransactionCaches(transaction.getId());
        return transaction;
    }

    private Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction not found"));
    }
}
