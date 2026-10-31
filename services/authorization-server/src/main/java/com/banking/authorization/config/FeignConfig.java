package com.banking.authorization.config;

import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class FeignConfig {

    /**
     * Retries failed requests 2 times (3 total attempts: 1 initial + 2 retries).
     */
    @Bean
    public Retryer feignRetryer() {
        log.info("Configuring Feign Retryer: 2 retries (maxAttempts = 3)");
        return new Retryer.Default(100L, TimeUnit.SECONDS.toMillis(1L), 3);
    }

    /**
     * Custom ErrorDecoder to convert 5xx errors into RetryableException,
     * allowing Feign to retry 2 times before triggering fallback.
     */
    @Bean
    public ErrorDecoder feignErrorDecoder() {
        final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            final int status = response.status();
            if (status >= 500) {
                log.warn("Feign call to '{}' returned HTTP {}. Triggering Feign retry.", methodKey, status);
                return new RetryableException(
                        status, "Server error (" + status + ") from user-service. Retrying...",
                        response.request().httpMethod(), (Long) null, response.request());
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }
}
