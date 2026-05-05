package com.team31.financetracker.account.adapter;

import com.team31.financetracker.account.dto.TopAccountDTO;
import org.springframework.stereotype.Component;

@Component
public class TopAccountProjectionAdapter {
    public TopAccountDTO adapt(Object[] row) {
        return TopAccountDTO.builder()
                .accountId(((Number) row[0]).longValue())
                .name((String) row[1])
                .balance(((Number) row[2]).doubleValue())
                .totalTransactions(((Number) row[3]).longValue())
                .build();
    }
}
