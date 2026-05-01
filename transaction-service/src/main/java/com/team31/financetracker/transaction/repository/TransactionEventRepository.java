package com.team31.financetracker.transaction.repository;

import com.team31.financetracker.transaction.mongodb.TransactionEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link TransactionEvent} documents
 * stored in the {@code transaction_events} collection.
 *
 * Used by
 * {@link com.team31.financetracker.transaction.observer.MongoEventLogger}
 * to persist events produced by the Observer chain (DP-2).
 */
@Repository
public interface TransactionEventRepository extends MongoRepository<TransactionEvent, String> {

    /**
     * Find all events belonging to a specific transaction, most-recent first.
     * Used by S3-F10 and test scenarios.
     */
    List<TransactionEvent> findByTransactionIdOrderByTimestampDesc(Long transactionId);
}
