package com.banking.authorization.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserAuthResponse(
        UUID id,
        String username,
        String password,
        boolean enabled,
        String role
) {
}
