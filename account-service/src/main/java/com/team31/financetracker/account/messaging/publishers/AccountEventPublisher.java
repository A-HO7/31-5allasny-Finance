package com.team31.financetracker.account.messaging.publishers;

import com.team31.financetracker.contracts.events.AccountRatedEvent;
import com.team31.financetracker.contracts.events.AccountStatsUpdatedEvent;
import com.team31.financetracker.contracts.events.AccountStatusChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
public class AccountEventPublisher {

    // Add this field after the class declaration:
    private static final Logger log = LoggerFactory.getLogger(AccountEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final String accountExchange;


    public AccountEventPublisher(RabbitTemplate rabbitTemplate, @Value("${rabbitmq.exchanges.account}") String accountExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.accountExchange = accountExchange;
    }

    public void publishAccountStatusChangedEvent(AccountStatusChangedEvent event) {
        MDC.put("routingKey", "account.status-changed");
        MDC.put("accountId", String.valueOf(event.accountId()));
        try {
            rabbitTemplate.convertAndSend(accountExchange, "account.status-changed", event);
            log.info("Published account.status-changed for accountId={}", event.accountId());
        } finally {
            MDC.remove("routingKey");
            MDC.remove("accountId");
        }
    }

    public void publishAccountRatedEvent(AccountRatedEvent event) {
        MDC.put("routingKey", "account.rated");
        MDC.put("accountId", String.valueOf(event.accountId()));
        try {
            rabbitTemplate.convertAndSend(accountExchange, "account.rated", event);
            log.info("Published account.rated for accountId={}", event.accountId());
        } finally {
            MDC.remove("routingKey");
            MDC.remove("accountId");
        }
    }

    public void publishAccountStatsUpdatedEvent(AccountStatsUpdatedEvent event) {
        MDC.put("routingKey", "account.stats-updated");
        MDC.put("accountId", String.valueOf(event.accountId()));
        try {
            rabbitTemplate.convertAndSend(accountExchange, "account.stats-updated", event);
            log.info("Published account.stats-updated for accountId={}", event.accountId());
        } finally {
            MDC.remove("routingKey");
            MDC.remove("accountId");
        }
    }
}
