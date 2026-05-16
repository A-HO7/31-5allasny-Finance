package com.team31.financetracker.account.config;

import feign.FeignException;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 404) {
                return new FeignException.NotFound(
                        "Resource not found",
                        response.request(),
                        null, null
                );
            }
            return new FeignException.ServiceUnavailable(
                    "Service unavailable",
                    response.request(),
                    null, null
            );
        };
    }

}
