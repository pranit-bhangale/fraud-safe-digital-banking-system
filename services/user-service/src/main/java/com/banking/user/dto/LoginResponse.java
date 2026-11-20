package com.banking.user.dto;

import lombok.Builder;

@Builder
public record LoginResponse(
        String username,
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
