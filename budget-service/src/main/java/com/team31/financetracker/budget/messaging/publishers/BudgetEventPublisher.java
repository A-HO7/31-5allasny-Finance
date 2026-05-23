package com.team31.financetracker.budget.messaging.publishers;

import com.team31.financetracker.budget.model.Budget;
import com.team31.financetracker.contracts.events.BudgetUsageUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class BudgetEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BudgetEventPublisher.class);

    private static final String BUDGET_EXCHANGE = "budget.events";
    private static final String USAGE_UPDATED_KEY = "budget.usage-updated";

    private final RabbitTemplate rabbitTemplate;

    public BudgetEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes a budget.usage-updated event to the budget.events exchange.
     * Called after spentAmount or status changes on a budget.
     */
    public void publishUsageUpdated(Budget budget) {
        BudgetUsageUpdatedEvent event = new BudgetUsageUpdatedEvent(
                budget.getId(),
                budget.getUserId(),
                budget.getCategory() != null ? budget.getCategory().name() : null,
                budget.getSpentAmount(),
                budget.getStatus() != null ? budget.getStatus().name() : null
        );

        try {
            rabbitTemplate.convertAndSend(BUDGET_EXCHANGE, USAGE_UPDATED_KEY, event);
            log.info("Published budget.usage-updated budgetId={} userId={} status={}",
                    budget.getId(), budget.getUserId(), budget.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish budget.usage-updated budgetId={}", budget.getId(), e);
        }
    }
}
