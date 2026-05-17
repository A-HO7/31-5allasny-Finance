package com.team31.financetracker.account;

import com.team31.financetracker.account.mongo.AccountEventRepository;
import com.team31.financetracker.account.repository.AccountSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:account-service-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration",
        "feign.account-service.url=http://account-service:8080",
        "feign.budget-service.url=http://budget-service:8080",
        "feign.reporting-service.url=http://reporting-service:8080",
        "feign.transaction-service.url=http://transaction-service:8080",
        "feign.user-service.url=http://user-service:8080",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class AccountServiceApplicationTests {

    @MockitoBean
    private AccountSearchRepository accountSearchRepository;

    @MockitoBean
    private AccountEventRepository accountEventRepository;

    @Test
    void contextLoads() {
    }

}
