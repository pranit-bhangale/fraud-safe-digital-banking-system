package com.banking.authorization.client;

import com.banking.authorization.config.FeignConfig;
import com.banking.authorization.dto.UserAuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        path = "/api/users",
        configuration = FeignConfig.class,
        fallbackFactory = UserClientFallback.class
)
public interface UserClient {

    @GetMapping("/internal/by-username/{username}")
    UserAuthResponse getUserByUsername(@PathVariable("username") String username);
}
