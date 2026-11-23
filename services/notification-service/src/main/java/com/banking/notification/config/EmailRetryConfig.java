package com.banking.notification.config;

import com.banking.notification.exception.EmailPermanentDeliveryException;
import com.banking.notification.exception.EmailRenderException;
import com.banking.notification.exception.EmailValidationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

import java.time.Duration;

@Configuration
public class EmailRetryConfig {

    @Bean
    public RetryTemplate processEmailRetryTemplate() {
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxRetries(4)
                .delay(Duration.ofSeconds(2))
                .multiplier(2.0)
                .maxDelay(Duration.ofSeconds(30))
                .excludes(
                        EmailPermanentDeliveryException.class,
                        EmailValidationException.class,
                        EmailRenderException.class)
                .build();
        return new RetryTemplate(retryPolicy);
    }
}