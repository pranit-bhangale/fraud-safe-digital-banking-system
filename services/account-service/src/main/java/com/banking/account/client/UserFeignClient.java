package com.banking.account.client;

import com.banking.account.config.FeignClientConfiguration;
import com.banking.account.dto.AccountDetail;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@FeignClient(
        name = "user-service",
        url = "${user.service.url:http://localhost:8081}",
        configuration = FeignClientConfiguration.class
)
public interface UserFeignClient {

    Logger LOG = LoggerFactory.getLogger(UserFeignClient.class);

    @GetMapping("/internal/users/{userId}/exists")
    @CircuitBreaker(name = "userService", fallbackMethod = "existsByUserIdFallback")
    @Retry(name = "userService")
    @TimeLimiter(name = "userService")
    CompletableFuture<Boolean> existsByUserId(@PathVariable UUID userId);

    @GetMapping("/internal/users/{userId}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "userService")
    @TimeLimiter(name = "userService")
    CompletableFuture<AccountDetail.UserInformation> getUserById(@PathVariable UUID userId);

    default CompletableFuture<Boolean> existsByUserIdFallback(UUID userId, Exception ex) {
        UserFeignClient.LOG.warn("Circuit breaker fallback for existsByUserId: userId={}, error={}", userId, ex.getMessage());
        return CompletableFuture.completedFuture(false);
    }

    default CompletableFuture<AccountDetail.UserInformation> getUserByIdFallback(UUID userId, Exception ex) {
        UserFeignClient.LOG.warn("Circuit breaker fallback for getUserById: userId={}, error={}", userId, ex.getMessage());
        return CompletableFuture.completedFuture(null);
    }
}