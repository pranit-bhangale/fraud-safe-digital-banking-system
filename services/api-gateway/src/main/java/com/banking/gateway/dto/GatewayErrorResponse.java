package com.banking.gateway.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record GatewayErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp
) {
}
