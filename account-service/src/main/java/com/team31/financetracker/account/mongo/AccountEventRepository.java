package com.team31.financetracker.account.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AccountEventRepository extends MongoRepository<AccountEvent, String> {
    List<AccountEvent> findByAccountIdOrderByTimestampDesc(Long accountId);
}
