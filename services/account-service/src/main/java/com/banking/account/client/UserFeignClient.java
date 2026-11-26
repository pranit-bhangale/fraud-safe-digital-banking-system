package com.banking.account.client;

import com.banking.account.config.FeignClientConfiguration;
import com.banking.account.dto.AccountDetail;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        url = "${user.service.url:http://localhost:8081}",
        configuration = FeignClientConfiguration.class
)
public interface UserFeignClient {

    @GetMapping(value = "/internal/users/{userId}/exists", version = "v2")
    boolean existsByUserId(@PathVariable UUID userId);

    @GetMapping(value = "/internal/users/{userId}", version = "v2")
    AccountDetail.UserInformation getUserById(@PathVariable UUID userId);
}