package com.team31.financetracker.account.adapter;

import com.team31.financetracker.account.dto.AccountDTO;
import com.team31.financetracker.account.model.AccountSearchDocument;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchHitAdapter {
    public AccountDTO adapt(AccountSearchDocument source) {
        return AccountDTO.builder()
                .id(Long.parseLong(source.getId()))
                .name(source.getName())
                // Convert String from Elasticsearch to AccountType Enum
                .type(source.getType() != null ? AccountType.valueOf(source.getType()) : null)
                .description(source.getDescription())
                .currency(source.getCurrency())
                .balance(source.getBalance())
                // Convert String from Elasticsearch to AccountStatus Enum
                .status(source.getStatus() != null ? AccountStatus.valueOf(source.getStatus()) : null)
                .rating(source.getRating())
                .build();
    }
}