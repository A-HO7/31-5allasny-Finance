package com.team31.financetracker.budget.repository;

import com.team31.financetracker.budget.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
