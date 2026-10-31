package com.banking.authorization.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserClientFallback implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(final Throwable cause) {
        return username -> {
            log.error("Get user by username Call failed for username '{}'. Cause: {}",
                    username, cause != null ? cause.getMessage() : "Unknown error");
            return null;
        };
    }
}
