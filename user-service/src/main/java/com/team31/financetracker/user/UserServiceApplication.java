package com.team31.financetracker.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableCaching
@EnableScheduling
@SpringBootApplication
@EnableFeignClients(basePackages = {
    "com.team31.financetracker.user",
    "com.team31.financetracker.contracts.feign"
})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
