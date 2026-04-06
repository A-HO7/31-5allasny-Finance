package com.team31.financetracker.user.repository;

import com.team31.financetracker.user.model.FinancialGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {
}