package com.banking.authorization.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record TokenValidationResponse(
        boolean valid,
        UUID userId,
        String username,
        List<String> roles,
        String message
) {
}
