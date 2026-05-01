package com.team31.financetracker.reporting.config;

import com.team31.financetracker.reporting.factory.EventType;
import com.team31.financetracker.reporting.mongo.ReportAuditEventRepository;
import com.team31.financetracker.reporting.observer.MongoEventLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObserverConfig {

    @Bean
    public MongoEventLogger mongoEventLogger(ReportAuditEventRepository repository) {
        return new MongoEventLogger(EventType.REPORT_AUDIT, repository);
    }

}
