package com.team31.financetracker.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team31.financetracker.transaction.Enums.TransactionCategory;
import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.Enums.TransactionType;
import com.team31.financetracker.transaction.dto.TransferEstimateRequest;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionEstimateIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    void estimateWithLowVolumeUsesHalfPercentFee() throws Exception {
        TransferEstimateRequest body = new TransferEstimateRequest(1L, 2L, 1000.0);
        mockMvc.perform(post("/api/transactions/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1000.0))
                .andExpect(jsonPath("$.feePercentage").value(0.5))
                .andExpect(jsonPath("$.transferFee").value(5.0))
                .andExpect(jsonPath("$.netTransfer").value(995.0));
    }

    @Test
    void estimateWithModerateVolumeUsesOnePercentFee() throws Exception {
        for (int i = 0; i < 15; i++) {
            transactionRepository.save(pendingTransfer(900.0 + i));
        }

        TransferEstimateRequest body = new TransferEstimateRequest(1L, 2L, 1000.0);
        mockMvc.perform(post("/api/transactions/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feePercentage").value(1.0))
                .andExpect(jsonPath("$.transferFee").value(10.0))
                .andExpect(jsonPath("$.netTransfer").value(990.0));
    }

    @Test
    void estimateDoesNotPersistTransactions() throws Exception {
        long before = transactionRepository.count();
        TransferEstimateRequest body = new TransferEstimateRequest(1L, 2L, 500.0);
        mockMvc.perform(post("/api/transactions/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body)))
                .andExpect(status().isOk());
        assertThat(transactionRepository.count()).isEqualTo(before);
    }

    @Test
    void estimateWithNonPositiveAmountReturns400() throws Exception {
        TransferEstimateRequest body = new TransferEstimateRequest(1L, 2L, 0.0);
        mockMvc.perform(post("/api/transactions/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void estimateWithMissingAccountReturns404() throws Exception {
        TransferEstimateRequest body = new TransferEstimateRequest(1L, 99L, 1000.0);
        mockMvc.perform(post("/api/transactions/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    private static Transaction pendingTransfer(double amount) {
        Transaction t = new Transaction();
        t.setAccountId(1L);
        t.setToAccountId(2L);
        t.setUserId(1L);
        t.setType(TransactionType.TRANSFER);
        t.setAmount(amount);
        t.setCurrency("EGP");
        t.setCategory(TransactionCategory.TRANSFER);
        t.setStatus(TransactionStatus.PENDING);
        t.setTransactionDate(LocalDateTime.of(2026, 6, 1, 12, 0));
        return t;
    }
}
