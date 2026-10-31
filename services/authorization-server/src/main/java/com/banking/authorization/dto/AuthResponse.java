package com.banking.authorization.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn,
        UUID userId,
        String username,
        List<String> roles
) {
}
