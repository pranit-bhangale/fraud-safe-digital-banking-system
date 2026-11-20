package com.banking.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configures a dedicated virtual thread executor for asynchronous email processing.
     * Creates a new virtual thread for each submitted email task with the "email-" prefix.
     * Automatically terminates individual virtual threads upon task completion.
     * Closes the executor gracefully when the Spring application context shuts down.
     */
    @Bean(name = "emailExecutor", destroyMethod = "close")
    public ExecutorService emailExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread
                .ofVirtual()
                .name("email-", 0)
                .factory());
    }
}