package com.banking.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@Schema(description = "Structured API Gateway error response")
public record GatewayErrorResponse(
        @Schema(description = "HTTP status code", example = "503")
        int status,

        @Schema(description = "HTTP status reason", example = "Service Unavailable")
        String error,

        @Schema(description = "Detailed failure or circuit breaker message", example = "User Service is temporarily unavailable. Circuit breaker open or service degraded.")
        String message,

        @Schema(description = "Request URI path", example = "/api/auth/login")
        String path,

        @Schema(description = "Timestamp when error occurred")
        Instant timestamp
) {
}
