package com.team31.financetracker.budget.repository;

import com.team31.financetracker.budget.model.BudgetEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BudgetEventRepository extends MongoRepository<BudgetEvent, String> {
}