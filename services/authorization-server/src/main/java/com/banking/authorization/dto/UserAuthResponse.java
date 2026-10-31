package com.banking.authorization.dto;

import java.util.UUID;

public record UserAuthResponse(
        UUID id,
        String username,
        String password,
        boolean enabled,
        String role
) {
}
