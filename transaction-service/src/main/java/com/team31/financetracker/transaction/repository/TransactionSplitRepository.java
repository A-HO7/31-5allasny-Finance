package com.team31.financetracker.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.team31.financetracker.transaction.model.TransactionSplit;

@Repository
public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, Long> {
}
