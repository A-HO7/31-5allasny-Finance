package com.team31.financetracker.transaction.repository;

import com.team31.financetracker.transaction.model.TransactionSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, Long> {

    /** Find all splits belonging to a given transaction, ordered by splitOrder. */
    List<TransactionSplit> findByTransactionIdOrderBySplitOrderAsc(Long transactionId);
}
