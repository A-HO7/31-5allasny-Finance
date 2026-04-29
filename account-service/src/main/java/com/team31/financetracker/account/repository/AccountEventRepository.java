package com.team31.financetracker.account.repository;

import com.team31.financetracker.account.event.AccountEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AccountEventRepository extends MongoRepository<AccountEvent, String> {
    List<AccountEvent> findByAccountIdOrderByTimestampDesc(Long accountId);
}
