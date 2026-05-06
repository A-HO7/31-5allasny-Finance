package com.team31.financetracker.user.config;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import java.io.File;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    private static final String DATABASE_NAME = "financemongo";

    @Override
    protected String getDatabaseName() {
        return DATABASE_NAME;
    }

    @Bean
    @Override
    public MongoClient mongoClient() {
        String host = isRunningInsideDocker() ? "mongo" : "localhost";

        String uri = "mongodb://root:rootpass@" + host + ":27017/"
                + DATABASE_NAME
                + "?authSource=admin";

        return MongoClients.create(new ConnectionString(uri));
    }

    private boolean isRunningInsideDocker() {
        return new File("/.dockerenv").exists();
    }
}