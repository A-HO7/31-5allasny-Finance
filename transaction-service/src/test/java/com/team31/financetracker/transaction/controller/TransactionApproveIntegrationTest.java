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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionApproveIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        transactionRepository.deleteAll();
        jdbcTemplate.update("UPDATE accounts SET balance = 2000.0 WHERE id = 1");
    }

    @Test
    void approvePendingExpenseUpdatesTransactionAndAccountBalance() throws Exception {
        Transaction pending = pendingExpense(500.0, 1L);
        Transaction saved = transactionRepository.save(pending);

        mockMvc.perform(put("/api/transactions/{transactionId}/approve", saved.getId())
                        .param("approverId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approverId").value(1))
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        Double balance = jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE id = ?", Double.class, 1L);
        assertThat(balance).isEqualTo(1500.0);
    }

    @Test
    void approveAgainReturns400() throws Exception {
        Transaction saved = transactionRepository.save(pendingExpense(500.0, 1L));

        mockMvc.perform(put("/api/transactions/{transactionId}/approve", saved.getId())
                        .param("approverId", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/transactions/{transactionId}/approve", saved.getId())
                        .param("approverId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveWithNonAdminReturns400() throws Exception {
        Transaction saved = transactionRepository.save(pendingExpense(100.0, 1L));

        mockMvc.perform(put("/api/transactions/{transactionId}/approve", saved.getId())
                        .param("approverId", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveNonExistentTransactionReturns404() throws Exception {
        mockMvc.perform(put("/api/transactions/{transactionId}/approve", 999_999L)
                        .param("approverId", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveWithNonExistentApproverReturns404() throws Exception {
        Transaction saved = transactionRepository.save(pendingExpense(50.0, 1L));

        mockMvc.perform(put("/api/transactions/{transactionId}/approve", saved.getId())
                        .param("approverId", "99999"))
                .andExpect(status().isNotFound());
    }

    private static Transaction pendingExpense(double amount, long accountId) {
        Transaction t = new Transaction();
        t.setAccountId(accountId);
        t.setUserId(1L);
        t.setType(TransactionType.EXPENSE);
        t.setAmount(amount);
        t.setCurrency("USD");
        t.setCategory(TransactionCategory.FOOD);
        t.setStatus(TransactionStatus.PENDING);
        t.setTransactionDate(LocalDateTime.of(2026, 4, 1, 12, 0));
        return t;
    }
}
