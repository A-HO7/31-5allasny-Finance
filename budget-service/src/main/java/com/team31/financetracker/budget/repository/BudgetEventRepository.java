package com.team31.financetracker.budget.repository;

import com.team31.financetracker.budget.model.BudgetEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BudgetEventRepository extends MongoRepository<BudgetEvent, String> {

    List<BudgetEvent> findByBudgetId(Long budgetId);

    List<BudgetEvent> findByAction(String action);

    List<BudgetEvent> findByBudgetIdAndAction(Long budgetId, String action);
}
