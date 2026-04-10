package com.team31.financetracker.transaction.controller;

import com.team31.financetracker.transaction.Enums.TransactionCategory;
import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.Enums.TransactionType;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionSearchIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        transactionRepository.deleteAll();
    }

    @Test
    void searchWithStatusReturnsCompletedInMarchOrderedNewestFirst() throws Exception {
        seedFiveTransactionsScenario();

        mockMvc.perform(get("/api/transactions/search")
                        .param("status", "COMPLETED")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].transactionDate").value("2026-03-20T12:00:00"))
                .andExpect(jsonPath("$[1].transactionDate").value("2026-03-10T12:00:00"));
    }

    @Test
    void searchWithoutStatusReturnsAllInMarch() throws Exception {
        seedFiveTransactionsScenario();

        mockMvc.perform(get("/api/transactions/search")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    private void seedFiveTransactionsScenario() {
        transactionRepository.save(tx(TransactionStatus.COMPLETED, LocalDateTime.of(2026, 3, 10, 12, 0)));
        transactionRepository.save(tx(TransactionStatus.COMPLETED, LocalDateTime.of(2026, 3, 20, 12, 0)));
        transactionRepository.save(tx(TransactionStatus.PENDING, LocalDateTime.of(2026, 3, 5, 12, 0)));
        transactionRepository.save(tx(TransactionStatus.COMPLETED, LocalDateTime.of(2026, 2, 15, 12, 0)));
        transactionRepository.save(tx(TransactionStatus.COMPLETED, LocalDateTime.of(2026, 2, 28, 12, 0)));
    }

    private static Transaction tx(TransactionStatus status, LocalDateTime transactionDate) {
        Transaction t = new Transaction();
        t.setAccountId(1L);
        t.setUserId(1L);
        t.setType(TransactionType.EXPENSE);
        t.setAmount(10.0);
        t.setCurrency("USD");
        t.setCategory(TransactionCategory.FOOD);
        t.setStatus(status);
        t.setTransactionDate(transactionDate);
        return t;
    }
}
