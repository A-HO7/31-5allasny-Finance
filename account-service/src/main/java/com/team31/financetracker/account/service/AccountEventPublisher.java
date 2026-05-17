package com.team31.financetracker.account.service;

import com.team31.financetracker.contracts.events.AccountRatedEvent;
import com.team31.financetracker.contracts.events.AccountStatsUpdatedEvent;
import com.team31.financetracker.contracts.events.AccountStatusChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AccountEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String accountExchange;

    public AccountEventPublisher(RabbitTemplate rabbitTemplate, @Value("${rabbitmq.exchanges.account}") String accountExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.accountExchange = accountExchange;
    }

    public void publishAccountStatusChangedEvent(AccountStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(accountExchange, "account.status-changed", event);
    }

    public void publishAccountRatedEvent(AccountRatedEvent event) {
        rabbitTemplate.convertAndSend(accountExchange, "account.rated", event);
    }

    public void publishAccountStatsUpdatedEvent(AccountStatsUpdatedEvent event) {
        rabbitTemplate.convertAndSend(accountExchange, "account.stats-updated", event);
    }
}
