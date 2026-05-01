package com.team31.financetracker.user.repository.nosql;

import com.team31.financetracker.user.model.nosql.AuthEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthEventRepository extends MongoRepository<AuthEvent, String> {
}