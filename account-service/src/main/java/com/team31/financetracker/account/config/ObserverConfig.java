package com.team31.financetracker.account.config;

import com.team31.financetracker.account.observer.MongoEventLogger;
import com.team31.financetracker.account.service.AccountService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObserverConfig {
    private final AccountService accountService;
    private final MongoEventLogger mongoEventLogger;

    public ObserverConfig(AccountService accountService, MongoEventLogger mongoEventLogger) {
        this.accountService = accountService;
        this.mongoEventLogger = mongoEventLogger;
    }

    @PostConstruct
    public void registerObservers() {
        accountService.register(mongoEventLogger);
    }
}
